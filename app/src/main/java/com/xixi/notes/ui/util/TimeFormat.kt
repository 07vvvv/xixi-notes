package com.xixi.notes.ui.util

import com.xixi.notes.R
import com.xixi.notes.data.local.TaskEntity
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Date
import java.util.Locale
import kotlin.math.max

private const val MINUTE_MS = 60_000L

/** 统一的日期时间格式：yyyy-MM-dd HH:mm（本地时区） */
private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

/** 格式化时间戳为 yyyy-MM-dd HH:mm */
fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

/** 系统本地化的完整日期时间（通知副标题等） */
fun formatLocalizedDateTime(millis: Long): String =
    DateFormat.getDateTimeInstance().format(Date(millis))

/** 剩余毫秒（可为负，表示已过期） */
fun remainingMillis(dueDate: Long, now: Long = System.currentTimeMillis()): Long =
    dueDate - now

/** 剩余时间的文字描述（中文，走 strings.xml） */
fun remainingTextRes(dueDate: Long, now: Long = System.currentTimeMillis()): RemainingText {
    val delta = remainingMillis(dueDate, now)
    if (delta <= 0L) return RemainingText(0L, RemainingUnit.OVERDUE)
    val minutes = max(1L, delta / MINUTE_MS)
    return when {
        minutes < 60L -> RemainingText(minutes, RemainingUnit.MINUTES)
        minutes < 60L * 24L -> RemainingText(minutes / 60L, RemainingUnit.HOURS)
        else -> RemainingText(minutes / (60L * 24L), RemainingUnit.DAYS)
    }
}

/** 剩余时间的单位 */
enum class RemainingUnit { MINUTES, HOURS, DAYS, OVERDUE }

/** 剩余时间描述 */
data class RemainingText(val value: Long, val unit: RemainingUnit) {
    /** 对应的字符串资源 id */
    val stringRes: Int
        get() = when (unit) {
            RemainingUnit.MINUTES -> R.string.remaining_minutes
            RemainingUnit.HOURS -> R.string.remaining_hours
            RemainingUnit.DAYS -> R.string.remaining_days
            RemainingUnit.OVERDUE -> R.string.overdue
        }
}

/** 是否已过期 */
fun isOverdue(dueDate: Long, now: Long = System.currentTimeMillis()): Boolean =
    dueDate < now

/** 本周一 00:00 的毫秒值（ISO 周，WeekFields.ISO 以周一为一周第一天） */
fun startOfIsoWeekMillis(
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone)
): Long {
    val monday = today.with(WeekFields.ISO.dayOfWeek(), 1L)
    return monday.atStartOfDay(zone).toInstant().toEpochMilli()
}

/** 本周日 23:59:59.999 的毫秒值 */
fun endOfIsoWeekMillis(
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone)
): Long {
    val sunday = today.with(WeekFields.ISO.dayOfWeek(), 1L).plusDays(6)
    return sunday.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
}

/** 判断任务是否在本周完成 */
fun isCompletedThisWeek(
    task: TaskEntity,
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone)
): Boolean {
    val completedAt = task.completedAt ?: return false
    return completedAt in startOfIsoWeekMillis(zone, today)..endOfIsoWeekMillis(zone, today)
}
