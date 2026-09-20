package com.xixi.notes.ui.util

import com.xixi.notes.data.local.TaskEntity

/**
 * 四象限优先级排序键：
 * 重要且紧急(0) > 不重要但紧急(1) > 重要但不紧急(2) > 不重要且不紧急(3)
 */
fun priorityRank(task: TaskEntity): Int = when {
    task.isImportant && task.isUrgent -> 0
    !task.isImportant && task.isUrgent -> 1
    task.isImportant && !task.isUrgent -> 2
    else -> 3
}

/** 组内主键：assignee 已勾选排前面 */
fun assigneeRank(task: TaskEntity): Int = if (task.isAssignedToMe) 0 else 1

/**
 * 轻重缓急模式下的组内比较器：
 * 主键 assignee，次键创建时间正序。
 */
fun priorityGroupComparator(): Comparator<TaskEntity> =
    compareBy<TaskEntity> { assigneeRank(it) }
        .thenBy { it.createdAt }

/**
 * 时间排序比较器：按 dueDate 升/降，无 dueDate 一律排最后。
 * assignee 规则同时生效（作为第一键）。
 */
fun timeComparator(ascending: Boolean): Comparator<TaskEntity> = Comparator { a, b ->
    val assigneeDiff = assigneeRank(a) - assigneeRank(b)
    if (assigneeDiff != 0) return@Comparator assigneeDiff

    val left = a.dueDate
    val right = b.dueDate
    when {
        left == null && right == null -> a.createdAt.compareTo(b.createdAt)
        left == null -> 1
        right == null -> -1
        ascending -> left.compareTo(right)
        else -> right.compareTo(left)
    }
}

/** 归档排序：completedAt ?: updatedAt 倒序 */
fun archiveComparator(): Comparator<TaskEntity> = Comparator { a, b ->
    val left = a.completedAt ?: a.updatedAt
    val right = b.completedAt ?: b.updatedAt
    right.compareTo(left)
}

/** 标题 / 描述是否命中关键词（图片不参与搜索） */
fun matchesQuery(task: TaskEntity, query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.trim()
    return task.title.contains(q, ignoreCase = true) ||
        (task.description?.contains(q, ignoreCase = true) == true)
}

/**
 * 搜索相关度：标题完全匹配(0) > 标题包含(1) > 描述包含(2) > 不匹配(3)
 */
fun searchRelevance(task: TaskEntity, query: String): Int {
    val q = query.trim()
    if (q.isEmpty()) return 0
    val title = task.title
    return when {
        title.equals(q, ignoreCase = true) -> 0
        title.contains(q, ignoreCase = true) -> 1
        task.description?.contains(q, ignoreCase = true) == true -> 2
        else -> 3
    }
}
