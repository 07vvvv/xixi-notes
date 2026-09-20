package com.xixi.notes.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks ORDER BY createdAt ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET isAssignedToMe = :value, updatedAt = :now WHERE id = :id")
    suspend fun setAssigned(id: Long, value: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isImportant = :value, updatedAt = :now WHERE id = :id")
    suspend fun setImportant(id: Long, value: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isUrgent = :value, updatedAt = :now WHERE id = :id")
    suspend fun setUrgent(id: Long, value: Boolean, now: Long = System.currentTimeMillis())

    /**
     * 全字段更新。
     *
     * 注意：[imagePathsJson] 是**已经序列化好的 JSON 文本**，直接写入 TEXT 列，
     * 不经过 [Converters]；调用前请先 `Json.encodeToString(imagePaths)`。
     */
    @Query(
        """
        UPDATE tasks SET
            title = :title, description = :description,
            isImportant = :isImportant, isUrgent = :isUrgent,
            isAssignedToMe = :isAssignedToMe,
            dueDate = :dueDate, reminderTime = :reminderTime,
            imagePaths = :imagePathsJson, updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateFields(
        id: Long,
        title: String,
        description: String?,
        isImportant: Boolean,
        isUrgent: Boolean,
        isAssignedToMe: Boolean,
        dueDate: Long?,
        reminderTime: Long?,
        imagePathsJson: String,
        updatedAt: Long
    )

    @Transaction
    suspend fun setCheckedOffWithTimestamp(id: Long, checked: Boolean) {
        val now = if (checked) System.currentTimeMillis() else null
        setCheckedOffAndCompletedAt(id, checked, now)
    }

    @Query(
        "UPDATE tasks SET isCheckedOff = :checked, completedAt = :completedAt, updatedAt = :now WHERE id = :id"
    )
    suspend fun setCheckedOffAndCompletedAt(
        id: Long,
        checked: Boolean,
        completedAt: Long?,
        now: Long = System.currentTimeMillis()
    )
}
