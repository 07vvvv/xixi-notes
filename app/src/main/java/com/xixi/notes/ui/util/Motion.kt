package com.xixi.notes.ui.util

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlin.math.abs

/** 默认弹性动画：状态切换 250-350ms 量级 */
fun <T> elasticSpring(): FiniteAnimationSpec<T> = spring(
    dampingRatio = 0.7f,
    stiffness = Spring.StiffnessMedium
)

/** 快速弹性（勾选框等小元件） */
fun <T> quickSpring(): FiniteAnimationSpec<T> = spring(
    dampingRatio = 0.6f,
    stiffness = 800f
)

/** 位移 / 尺寸过渡（禁止 overshoot） */
fun <T> gentleTween(durationMillis: Int = 300): FiniteAnimationSpec<T> =
    tween(durationMillis = durationMillis, easing = GentleEasing)

/**
 * cubic-bezier(0.22, 1, 0.36, 1)
 * 该曲线的 y 分量恒不超过 1，因此不会产生 overshoot。
 */
val GentleEasing: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/** liq-create 面板展开：480ms 同曲线 */
const val LIQ_EXPAND_DURATION = 480

/** 浮点近似相等 */
fun Float.approx(other: Float, epsilon: Float = 0.01f): Boolean = abs(this - other) <= epsilon
