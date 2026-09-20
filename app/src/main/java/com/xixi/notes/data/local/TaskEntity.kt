package com.xixi.notes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 任务实体。
 *
 * 每任务两个标签（重要 / 紧急）可自由组合，共四种优先级。
 * [imagePaths] 只保存相对路径（如 "12/xxxx.jpg"），通过 TypeConverter 以 JSON 存入 TEXT 列。
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val isImportant: Boolean = false,
    val isUrgent: Boolean = false,
    val isAssignedToMe: Boolean = false,
    val isCheckedOff: Boolean = false,
    val dueDate: Long? = null,
    val reminderTime: Long? = null,
    val imagePaths: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
