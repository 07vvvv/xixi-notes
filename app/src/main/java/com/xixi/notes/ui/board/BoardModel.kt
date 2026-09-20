package com.xixi.notes.ui.board

import androidx.annotation.StringRes
import com.xixi.notes.R
import com.xixi.notes.data.local.TaskEntity

/**
 * 四象限、按优先级排序：
 * 重要且紧急 > 不重要但紧急 > 重要但不紧急 > 不重要且不紧急
 */
enum class Quadrant(
    @StringRes val titleRes: Int,
    @StringRes val shortTitleRes: Int
) {
    IMPORTANT_URGENT(R.string.quadrant_important_urgent, R.string.quadrant_important_urgent_short),
    URGENT_ONLY(R.string.quadrant_urgent_only, R.string.quadrant_urgent_only_short),
    IMPORTANT_ONLY(R.string.quadrant_important_only, R.string.quadrant_important_only_short),
    NEITHER(R.string.quadrant_neither, R.string.quadrant_neither_short);

    /** 分组 key，用于 DataStore 折叠状态；跨版本保持稳定 */
    val groupKey: String
        get() = when (this) {
            IMPORTANT_URGENT -> "important_urgent"
            URGENT_ONLY -> "urgent_only"
            IMPORTANT_ONLY -> "important_only"
            NEITHER -> "neither"
        }

    companion object {
        /** 展示与排序顺序：重要且紧急 -> 不重要但紧急 -> 重要但不紧急 -> 不重要且不紧急 */
        val ordered: List<Quadrant> = listOf(
            IMPORTANT_URGENT,
            URGENT_ONLY,
            IMPORTANT_ONLY,
            NEITHER
        )

        fun of(task: TaskEntity): Quadrant = when {
            task.isImportant && task.isUrgent -> IMPORTANT_URGENT
            !task.isImportant && task.isUrgent -> URGENT_ONLY
            task.isImportant && !task.isUrgent -> IMPORTANT_ONLY
            else -> NEITHER
        }
    }
}

/** 排序模式 */
enum class SortMode {
    /** 轻重缓急（默认）：四分组，可独立折叠 */
    PRIORITY,

    /** 时间正序：按 dueDate 升序，无 dueDate 排最后 */
    TIME_ASC,

    /** 时间倒序：按 dueDate 降序，无 dueDate 排最后 */
    TIME_DESC
}

/** 「我来做」（assignee）规则，三种排序模式通用 */
enum class AssigneeMode {
    /** 已勾选优先 */
    ASSIGNEE_FIRST,

    /** 仅视觉区分（排序不变） */
    VISUAL_ONLY,

    /** 分隔线区分（默认） */
    SEPARATED
}

/** 已完成任务的显示方式 */
enum class CompletedStyle {
    /** 原位显示，删除线 + 40% 透明 */
    IN_PLACE,

    /** 隐藏，通过顶部栏眼睛图标找回 */
    HIDDEN
}

/** 一个可显示的任务：实体 + 派生信息 */
data class TaskUi(
    val task: TaskEntity,
    val quadrant: Quadrant
)

/** 轻重缓急模式下的一个分组 */
data class TaskQuadrantGroup(
    val quadrant: Quadrant,
    val tasks: List<TaskEntity>,
    val separatedAssigned: List<TaskEntity>,
    val isFolded: Boolean
)
