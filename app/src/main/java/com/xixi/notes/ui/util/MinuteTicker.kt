package com.xixi.notes.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

/**
 * 每分钟刷新的「当前时间」。
 *
 * 用于任务行的剩余时间显示：到期时间变化后一分钟内自动更新。
 * 只在需要的地方调用，避免全局重组。
 */
@Composable
fun rememberMinuteTick(): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            now.longValue = current
            // 对齐到下一分钟边界（多等 50ms 避免边界抖动）
            val untilNextMinute = 60_000L - (current % 60_000L)
            delay(untilNextMinute + 50L)
        }
    }
    return now
}

/** 便捷获取当前分钟时间戳 */
@Composable
fun currentMinute(): Long {
    val tick by rememberMinuteTick()
    return tick
}
