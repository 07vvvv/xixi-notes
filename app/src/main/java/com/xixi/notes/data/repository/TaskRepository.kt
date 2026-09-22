package com.xixi.notes.data.repository

import com.xixi.notes.data.local.TaskDao
import com.xixi.notes.data.local.TaskEntity
import com.xixi.notes.image.ImageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 新建 / 编辑任务的入参（图片为「待保存的临时相对路径列表」） */
data class TaskDraft(
    val title: String,
    val description: String?,
    val isImportant: Boolean,
    val isUrgent: Boolean,
    val isAssignedToMe: Boolean,
    val isCheckedOff: Boolean,
    val dueDate: Long?,
    val reminderTime: Long?,
    /** 当前编辑状态下展示的全部图片路径（已保存的正式路径 + 新选的临时路径） */
    val imagePaths: List<String>
)

/**
 * 任务仓库：唯一的数据入口。
 *
 * - 数据库与图片文件操作的顺序：**先删数据库记录，再删图片文件夹**
 * - 新建任务时把临时图片移动到正式目录，失败回滚
 */
class TaskRepository(
    private val dao: TaskDao,
    private val imageManager: ImageManager
) {

    val allTasks: Flow<List<TaskEntity>> = dao.getAllTasks()

    suspend fun getTaskById(id: Long): TaskEntity? = dao.getTaskById(id)

    suspend fun setAssigned(id: Long, value: Boolean) = dao.setAssigned(id, value)

    suspend fun setImportant(id: Long, value: Boolean) = dao.setImportant(id, value)

    suspend fun setUrgent(id: Long, value: Boolean) = dao.setUrgent(id, value)

    suspend fun setCheckedOff(id: Long, checked: Boolean) =
        dao.setCheckedOffWithTimestamp(id, checked)

    /**
     * 新建任务：
     * 1. insert 拿到 taskId
     * 2. 把临时图片移动到 filesDir/images/{taskId}/
     * 3. 序列化 imagePaths 后 updateFields 写入（直接写 TEXT 列，不经 TypeConverter）
     *
     * 任一步失败都会删除已插入的记录与已移动的文件（尽力回滚）。
     */
    suspend fun createTask(draft: TaskDraft): Long = withContext(Dispatchers.IO) {
        val entity = TaskEntity(
            title = draft.title,
            description = draft.description,
            isImportant = draft.isImportant,
            isUrgent = draft.isUrgent,
            isAssignedToMe = draft.isAssignedToMe,
            isCheckedOff = draft.isCheckedOff,
            dueDate = draft.dueDate,
            reminderTime = draft.reminderTime,
            imagePaths = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            completedAt = if (draft.isCheckedOff) System.currentTimeMillis() else null
        )
        val taskId = dao.insert(entity)
        if (taskId <= 0L) throw IllegalStateException("任务插入失败")

        val moved = mutableListOf<String>()
        try {
            for (tempPath in draft.imagePaths) {
                moved += imageManager.moveToTask(tempPath, taskId)
            }
            dao.updateFields(
                id = taskId,
                title = draft.title,
                description = draft.description,
                isImportant = draft.isImportant,
                isUrgent = draft.isUrgent,
                isAssignedToMe = draft.isAssignedToMe,
                dueDate = draft.dueDate,
                reminderTime = draft.reminderTime,
                imagePathsJson = Json.encodeToString(moved),
                updatedAt = System.currentTimeMillis()
            )
            taskId
        } catch (e: Exception) {
            // 回滚：删掉已移动的图片文件与刚插入的记录
            moved.forEach { imageManager.deleteImage(it) }
            imageManager.deleteTaskImages(taskId)
            runCatching { dao.delete(entity.copy(id = taskId)) }
            throw e
        }
    }

    /**
     * 编辑任务：
     * - [newTempPaths] 需要从临时目录移到任务目录的新图片
     * - [removedPaths] 需要删除文件的旧图片
     * - [finalPaths] 最终展示顺序的路径列表（其中新图片仍是临时路径，写入前会替换为正式路径）
     */
    suspend fun updateTask(
        id: Long,
        draft: TaskDraft,
        newTempPaths: List<String>,
        removedPaths: List<String>,
        finalPaths: List<String>
    ): Unit = withContext(Dispatchers.IO) {
        val moved = mutableListOf<String>()
        try {
            for (tempPath in newTempPaths) {
                moved += imageManager.moveToTask(tempPath, id)
            }
            // 新图片已移动，写入数据库前把临时路径替换为正式相对路径
            val movedMap = newTempPaths.zip(moved).toMap()
            val persistedPaths = finalPaths.map { movedMap[it] ?: it }

            dao.updateFields(
                id = id,
                title = draft.title,
                description = draft.description,
                isImportant = draft.isImportant,
                isUrgent = draft.isUrgent,
                isAssignedToMe = draft.isAssignedToMe,
                dueDate = draft.dueDate,
                reminderTime = draft.reminderTime,
                imagePathsJson = Json.encodeToString(persistedPaths),
                updatedAt = System.currentTimeMillis()
            )
            // updateFields 不含完成状态，单独同步（同时维护 completedAt）
            dao.setCheckedOffWithTimestamp(id, draft.isCheckedOff)
        } catch (e: Exception) {
            moved.forEach { imageManager.deleteImage(it) }
            throw e
        }
        // 数据库写入成功后再删除被移除的图片文件
        removedPaths.forEach { path ->
            if (path !in finalPaths) runCatching { imageManager.deleteImage(path) }
        }
    }

    /** 先删数据库记录，再删图片文件夹 */
    suspend fun deleteTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        dao.delete(task)
        runCatching { imageManager.deleteTaskImages(task.id) }
    }

    /** 只删数据库记录（延迟删除场景，图片由 [deleteTaskImages] 稍后清理） */
    suspend fun deleteTaskRecord(task: TaskEntity) = withContext(Dispatchers.IO) {
        dao.delete(task)
    }

    /** 删除任务图片文件夹 */
    suspend fun deleteTaskImages(taskId: Long) = withContext(Dispatchers.IO) {
        runCatching { imageManager.deleteTaskImages(taskId) }
    }

    /** 把任务重新插回数据库（撤销删除） */
    suspend fun restoreTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        dao.insert(task)
    }
}
