package com.xixi.notes.ui.util

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * cubic-bezier(0.22, 1, 0.36, 1)
 * 该曲线的 y 分量恒不超过 1，因此不会产生 overshoot。
 */
val GentleEasing: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/** liq-create 面板展开：480ms 同曲线 */
const val LIQ_EXPAND_DURATION = 480
