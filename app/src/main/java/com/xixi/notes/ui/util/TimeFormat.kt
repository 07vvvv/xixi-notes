package com.xixi.notes.ui.util

import com.xixi.notes.data.local.TaskEntity
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Date
import java.util.Locale

/** 统一的日期时间格式：yyyy-MM-dd HH:mm（本地时区） */
private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

/** 任务行右侧使用的紧凑格式：MM-dd HH:mm（如 09-22 14:30） */
private val shortDateTimeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

/** 格式化时间戳为 yyyy-MM-dd HH:mm */
fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

/** 格式化时间戳为 MM-dd HH:mm（任务行右侧截止时间） */
fun formatShortDateTime(millis: Long): String = shortDateTimeFormat.format(Date(millis))

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
