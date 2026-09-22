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
    /** 短标题（扇形菜单等空间受限处使用） */
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

/**
 * 排序模式。
 *
 * - [PRIORITY] 轻重缓急排序：四分组，可独立折叠
 * - [DUE_DATE] 按截止日期：dueDate 升序，无 dueDate 排最后
 * - [CREATED_AT] 按创建时间：createdAt 升序
 *
 * 兼容说明：历史版本的 TIME_ASC / TIME_DESC 在读取偏好时统一映射为 [DUE_DATE]
 * （见 AppPreferences 的 toSortMode 解析），因此枚举中不再保留这两个常量。
 */
enum class SortMode {
    /** 轻重缓急（默认） */
    PRIORITY,

    /** 按截止日期 */
    DUE_DATE,

    /** 按创建时间 */
    CREATED_AT
}

/** 已完成任务的显示方式 */
enum class CompletedStyle {
    /** 原位显示，删除线 + 40% 透明 */
    IN_PLACE,

    /** 隐藏，通过顶部栏眼睛图标找回 */
    HIDDEN
}

/** 轻重缓急模式下的一个分组 */
data class TaskQuadrantGroup(
    val quadrant: Quadrant,
    val tasks: List<TaskEntity>,
    val isFolded: Boolean
)
