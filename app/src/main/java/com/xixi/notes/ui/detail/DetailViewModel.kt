package com.xixi.notes.ui.detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xixi.notes.data.local.TaskEntity
import com.xixi.notes.data.preferences.AppPreferences
import com.xixi.notes.data.repository.TaskDraft
import com.xixi.notes.data.repository.TaskRepository
import com.xixi.notes.image.ImageManager
import com.xixi.notes.reminder.TaskReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** 编辑页中的一张图片（staged = 还未写入正式目录） */
data class EditorImageUi(
    val path: String,
    val staged: Boolean
)

/** 编辑页 UI 状态 */
data class DetailUiState(
    val loading: Boolean = true,
    val taskId: Long = -1L,
    val isNew: Boolean = true,
    val title: String = "",
    val description: String = "",
    val isImportant: Boolean = false,
    val isUrgent: Boolean = false,
    val isAssignedToMe: Boolean = false,
    val isCheckedOff: Boolean = false,
    val dueDate: Long? = null,
    val reminderTime: Long? = null,
    val images: List<EditorImageUi> = emptyList(),
    val compressing: Boolean = false,
    val compressionCurrent: Int = 0,
    val compressionTotal: Int = 0,
    val saving: Boolean = false,
    val deleteConfirmVisible: Boolean = false,
    val discardConfirmVisible: Boolean = false,
    val imageMenuVisible: Boolean = false,
    val message: String? = null,
    /** 是否有未保存修改（由 ViewModel 计算后写入） */
    val hasChanges: Boolean = false
) {
    /** 标题非空 且 有字段变化时启用保存 */
    val canSave: Boolean
        get() = !saving && !loading && title.isNotBlank() && hasChanges

    val remainingImageSlots: Int get() = (5 - images.size).coerceAtLeast(0)
}

class DetailViewModel(
    private val repository: TaskRepository,
    private val imageManager: ImageManager,
    private val preferences: AppPreferences,
    private val reminderScheduler: TaskReminderScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    /** 原始任务快照（用于比较字段变化） */
    private var original: TaskEntity? = null

    /** 本次编辑会话新建的临时图片（取消时清理） */
    private val newTempPaths = mutableSetOf<String>()

    /** 本次编辑会话被移除的图片（保存时删文件） */
    private val removedPaths = mutableSetOf<String>()

    /** 进入编辑会话时已存在的临时图片（保留，不清理） */
    /** 初始象限（Radial Menu 预置，仅对新建有意义） */
    private var initialQuadrant: com.xixi.notes.ui.board.Quadrant? = null

    // ------------------------------------------------------------ 载入

    fun load(taskId: Long) {
        viewModelScope.launch {
            if (taskId <= 0L) {
                original = null
                _state.value = DetailUiState(
                    loading = false,
                    taskId = -1L,
                    isNew = true,
                    isImportant = initialQuadrant?.let {
                        it == com.xixi.notes.ui.board.Quadrant.IMPORTANT_URGENT ||
                            it == com.xixi.notes.ui.board.Quadrant.IMPORTANT_ONLY
                    } ?: false,
                    isUrgent = initialQuadrant?.let {
                        it == com.xixi.notes.ui.board.Quadrant.IMPORTANT_URGENT ||
                            it == com.xixi.notes.ui.board.Quadrant.URGENT_ONLY
                    } ?: false
                )
                return@launch
            }
            val task = repository.getTaskById(taskId)
            if (task == null) {
                _state.value = DetailUiState(loading = false, isNew = false, taskId = taskId)
                return@launch
            }
            original = task
            _state.value = DetailUiState(
                loading = false,
                taskId = task.id,
                isNew = false,
                title = task.title,
                description = task.description.orEmpty(),
                isImportant = task.isImportant,
                isUrgent = task.isUrgent,
                isAssignedToMe = task.isAssignedToMe,
                isCheckedOff = task.isCheckedOff,
                dueDate = task.dueDate,
                reminderTime = task.reminderTime,
                images = task.imagePaths.map { EditorImageUi(it, staged = false) },
                hasChanges = false
            )
        }
    }

    fun setInitialQuadrant(quadrant: com.xixi.notes.ui.board.Quadrant?) {
        initialQuadrant = quadrant
    }

    // ------------------------------------------------------------ 字段编辑

    fun setTitle(value: String) {
        // 标题最多 120 字
        update { it.copy(title = value.take(120)) }
    }

    fun setDescription(value: String) {
        // 描述最多 500 字
        update { it.copy(description = value.take(500)) }
    }

    fun toggleImportant() = update { it.copy(isImportant = !it.isImportant) }

    fun toggleUrgent() = update { it.copy(isUrgent = !it.isUrgent) }

    fun toggleAssigned() = update { it.copy(isAssignedToMe = !it.isAssignedToMe) }

    fun toggleCheckedOff() = update { it.copy(isCheckedOff = !it.isCheckedOff) }

    /** 设置截止日期；未保存的任务不写入数据库 */
    fun setDueDate(value: Long?) = update { it.copy(dueDate = value) }

    /**
     * 设置提醒时间；未保存的任务不写入数据库。
     * 已有任务的提醒在保存时统一重设。
     */
    fun setReminderTime(value: Long?) = update { it.copy(reminderTime = value) }

    fun showDeleteConfirm() = update { it.copy(deleteConfirmVisible = true) }

    fun hideDeleteConfirm() = update { it.copy(deleteConfirmVisible = false) }

    fun showDiscardConfirm() = update { it.copy(discardConfirmVisible = true) }

    fun hideDiscardConfirm() = update { it.copy(discardConfirmVisible = false) }

    fun showImageMenu() = update { it.copy(imageMenuVisible = true) }

    fun hideImageMenu() = update { it.copy(imageMenuVisible = false) }

    fun clearMessage() = update { it.copy(message = null) }

    private fun update(transform: (DetailUiState) -> DetailUiState) {
        val next = transform(_state.value)
        _state.value = next.copy(hasChanges = computeHasChanges(next))
    }

    /** 未保存修改检测：对比原字段 + 图片列表 */
    private fun computeHasChanges(current: DetailUiState): Boolean {
        val base = original
        if (base == null) {
            // 新建：有任何内容即算修改（图片也会算）
            return current.title.isNotBlank() ||
                current.description.isNotBlank() ||
                current.isImportant ||
                current.isUrgent ||
                current.isAssignedToMe ||
                current.isCheckedOff ||
                current.dueDate != null ||
                current.reminderTime != null ||
                current.images.isNotEmpty()
        }
        return current.title != base.title ||
            current.description != base.description.orEmpty() ||
            current.isImportant != base.isImportant ||
            current.isUrgent != base.isUrgent ||
            current.isAssignedToMe != base.isAssignedToMe ||
            current.isCheckedOff != base.isCheckedOff ||
            current.dueDate != base.dueDate ||
            current.reminderTime != base.reminderTime ||
            current.images.map { it.path } != base.imagePaths
    }

    // ------------------------------------------------------------ 图片

    /** 相册选图：只取前 [remaining] 张；剩余 >= 2 走多选，== 1 走单选 */
    fun addFromGallery(uris: List<Uri>) {
        val remaining = _state.value.remainingImageSlots
        if (remaining <= 0) return
        val picked = uris.take(remaining)
        compressSequentially(picked)
    }

    /** 拍照完成后压缩临时文件 */
    fun addFromCamera(tempFile: java.io.File) {
        if (_state.value.remainingImageSlots <= 0) return
        compressSequentially(listOf(tempFile))
    }

    /** 顺序压缩（不并发），单张超时 30 秒并清理临时文件 */
    private fun compressSequentially(sources: List<Any>) {
        if (sources.isEmpty()) return
        update {
            it.copy(
                compressing = true,
                compressionCurrent = 0,
                compressionTotal = sources.size
            )
        }
        viewModelScope.launch {
            var failed = 0
            var index = 0
            for (source in sources) {
                index += 1
                _state.value = _state.value.copy(
                    compressionCurrent = index - 1,
                    compressionTotal = sources.size
                )
                val result = withTimeoutOrNull(ImageManager.COMPRESS_TIMEOUT_MS) {
                    when (source) {
                        is Uri -> imageManager.compressAndSaveToTemp(source)
                        is java.io.File -> imageManager.compressAndSaveToTemp(source)
                        else -> Result.failure(IllegalArgumentException("未知的图片来源"))
                    }
                }
                if (result == null || result.isFailure) {
                    // 超时或失败：清理源临时文件
                    failed += 1
                    if (source is java.io.File) source.delete()
                } else {
                    val relative = result.getOrNull()
                    if (relative.isNullOrBlank()) {
                        failed += 1
                        if (source is java.io.File) source.delete()
                    } else {
                        newTempPaths += relative
                        _state.value = _state.value.let { current ->
                            current.copy(
                                images = current.images + EditorImageUi(relative, staged = true)
                            )
                        }
                        // 相机源文件用完后删除
                        if (source is java.io.File) source.delete()
                    }
                }
                _state.value = _state.value.copy(compressionCurrent = index)
            }
            val total = sources.size
            val message = when {
                failed == 0 -> null
                total == 1 -> "图片处理失败"
                else -> "$total 张中 $failed 张处理失败"
            }
            update {
                it.copy(
                    compressing = false,
                    compressionCurrent = total,
                    message = message
                )
            }
        }
    }

    /** 删除单张图片：立即删文件（临时图片），正式图片等保存时再删 */
    fun removeImage(path: String) {
        val image = _state.value.images.firstOrNull { it.path == path } ?: return
        if (image.staged) {
            // 临时图片：立即删文件并清理记录
            newTempPaths -= path
            viewModelScope.launch { imageManager.deleteTempFile(path) }
        } else {
            // 已保存的图片：进入待删列表，保存时删除文件
            removedPaths += path
        }
        update { current ->
            current.copy(images = current.images.filterNot { it.path == path })
        }
    }

    /** 进入全屏查看：返回初始页下标 */
    fun openViewer(index: Int, onOpen: (Int) -> Unit) {
        onOpen(index)
    }

    // ------------------------------------------------------------ 保存 / 取消

    fun save(onDone: (Long) -> Unit) {
        val current = _state.value
        if (current.title.isBlank() || current.saving || !current.canSave) return
        _state.value = current.copy(saving = true)

        viewModelScope.launch {
            try {
                val draft = TaskDraft(
                    title = current.title.trim(),
                    description = current.description.ifBlank { null },
                    isImportant = current.isImportant,
                    isUrgent = current.isUrgent,
                    isAssignedToMe = current.isAssignedToMe,
                    isCheckedOff = current.isCheckedOff,
                    dueDate = current.dueDate,
                    reminderTime = current.reminderTime,
                    imagePaths = current.images.map { it.path }
                )
                val savedId: Long
                if (current.isNew) {
                    // 新建：insert 拿 taskId -> 移临时图片 -> updateFields（仓库内已用事务包装）
                    savedId = repository.createTask(draft)
                    newTempPaths.clear()
                } else {
                    val basePaths = original?.imagePaths.orEmpty()
                    val newTemp = current.images.filter { it.staged }.map { it.path }
                    val finalPaths = current.images.map { it.path }
                    repository.updateTask(
                        id = current.taskId,
                        draft = draft,
                        newTempPaths = newTemp,
                        removedPaths = removedPaths.toList(),
                        finalPaths = finalPaths
                    )
                    newTempPaths -= newTemp.toSet()
                    removedPaths.clear()
                    savedId = current.taskId
                }

                // 提醒生命周期：修改重设 / 完成取消
                syncReminder(savedId, draft)
                onDone(savedId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    saving = false,
                    message = "保存失败：${e.message ?: "未知错误"}"
                )
            }
        }
    }

    private suspend fun syncReminder(taskId: Long, draft: TaskDraft) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                if (draft.isCheckedOff || draft.reminderTime == null) {
                    reminderScheduler.cancel(taskId)
                } else {
                    reminderScheduler.schedule(taskId, draft.reminderTime)
                }
            }
        }
    }

    /** 删除任务：先删数据库记录，再删图片文件夹 */
    fun deleteTask(onDone: () -> Unit) {
        val current = _state.value
        viewModelScope.launch {
            if (!current.isNew) {
                val task = repository.getTaskById(current.taskId)
                if (task != null) {
                    runCatching { reminderScheduler.cancel(task.id) }
                    repository.deleteTask(task)
                }
            }
            // 本次会话新增的临时图片
            newTempPaths.forEach { imageManager.deleteTempFile(it) }
            newTempPaths.clear()
            onDone()
        }
    }

    /** 取消返回：清理本次编辑产生的临时图片 */
    fun cancel(onDone: () -> Unit) {
        viewModelScope.launch {
            newTempPaths.forEach { imageManager.deleteTempFile(it) }
            newTempPaths.clear()
            onDone()
        }
    }

    class Factory(
        private val repository: TaskRepository,
        private val imageManager: ImageManager,
        private val preferences: AppPreferences,
        private val reminderScheduler: TaskReminderScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DetailViewModel(repository, imageManager, preferences, reminderScheduler) as T
    }
}
