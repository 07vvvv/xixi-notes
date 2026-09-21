package com.xixi.notes.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xixi.notes.ui.theme.ThemeMode
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.GentleEasing
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/** 球直径 */
private val ORB = 36.dp

/** 相邻球心间距 */
private val PITCH = 44.dp

/**
 * 三颗球固定颜色：跟随系统 / 深色 / 浅色。
 *
 * 用 @Composable 函数（而不是带 `@Composable get()` 的属性）读取强调色板，
 * 语义更直白，也不会踩"属性上标注 @Composable"这类编译限制。
 */
@Composable
private fun followSystemColor(): Color = XixiTheme.accent.followSystem

@Composable
private fun darkModeColor(): Color = XixiTheme.accent.dark

@Composable
private fun lightModeColor(): Color = XixiTheme.accent.light

/** 每步延迟 */
private const val STEP_DELAY_MS = 22

/**
 * Magnet-Select：三个 36dp 圆球，间距 44dp，点击循环三态。
 *
 * - `grow = 1.16 + 0.22 * pull`
 * - `push = room + aura * fall`
 * - `fall = exp(-(far - 1) / 1.8)`
 * - 选中球先宽后高，远处球每步延迟 22ms
 */
@Composable
fun MagnetSelect(
    mode: ThemeMode,
    onModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(ThemeMode.FOLLOW_SYSTEM, ThemeMode.DARK, ThemeMode.LIGHT)
    val colors = listOf(followSystemColor(), darkModeColor(), lightModeColor())
    val selectedIndex = options.indexOf(mode).coerceAtLeast(0)

    Layout(
        modifier = modifier.onGloballyPositioned { },
        content = {
            options.forEachIndexed { index, option ->
                val selectedDistance = abs(index - selectedIndex)
                val fall = exp(-(selectedDistance - 1).coerceAtLeast(0).toFloat() / 1.8f)

                // 延迟：远处球每步 22ms
                val delay = selectedDistance * STEP_DELAY_MS
                // grow = 1.16 + 0.22 * pull（pull 随距离衰减）
                val pull = if (selectedDistance == 0) 1f else fall * 0.4f
                val targetGrow = 1.16f + 0.22f * pull
                // push = room + aura * fall
                val targetPush = if (selectedDistance == 0) 0f else (2.dp.value + 3.dp.value * fall)

                val grow by animateFloatAsState(
                    targetValue = targetGrow,
                    animationSpec = tween(durationMillis = 250, delayMillis = delay, easing = GentleEasing),
                    label = "magnet_grow_$index"
                )
                val push by animateFloatAsState(
                    targetValue = targetPush,
                    animationSpec = tween(durationMillis = 250, delayMillis = delay, easing = GentleEasing),
                    label = "magnet_push_$index"
                )

                // 选中球「先宽后高」：宽度立即放大，高度稍慢
                val selected = selectedDistance == 0
                val scaleX by animateFloatAsState(
                    targetValue = if (selected) grow * 1.06f else grow,
                    animationSpec = tween(durationMillis = 200, delayMillis = delay, easing = GentleEasing),
                    label = "magnet_scale_x_$index"
                )
                val scaleY by animateFloatAsState(
                    targetValue = if (selected) grow * 0.96f else grow,
                    animationSpec = tween(durationMillis = 260, delayMillis = delay + 40, easing = GentleEasing),
                    label = "magnet_scale_y_$index"
                )

                Box(
                    modifier = Modifier
                        .size(ORB)
                        .clip(CircleShape)
                        .background(colors[index])
                        .graphicsLayer {
                            this.scaleX = scaleX
                            this.scaleY = scaleY
                            this.translationX = (index - selectedIndex) * push
                        }
                        .clickableNoRipple {
                            val next = options[(options.indexOf(mode) + 1) % options.size]
                            onModeChange(next)
                        }
                )
            }
        }
    ) { measurables, constraints ->
        // MeasureScope 本身是 Density，可直接换算 dp -> px
        val orbPx = ORB.roundToPx()
        val pitchPx = PITCH.roundToPx()
        val width = orbPx + (measurables.size - 1) * pitchPx
        val height = orbPx

        val placeables = measurables.map { measurable ->
            measurable.measure(Constraints.fixed(orbPx, orbPx))
        }

        val layoutWidth = width.coerceAtMost(constraints.maxWidth.takeIf { it != Constraints.Infinity } ?: width)
        layout(layoutWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                placeable.place(x = index * pitchPx, y = 0)
            }
        }
    }
}

/** 供外部读取球组高度，保持与 FAB 位置计算一致 */
val MagnetSelectHeight: Dp = ORB

/** 触摸/点击目标的最小尺寸（无障碍） */
val MagnetSelectTouchSize: Dp = max(ORB.value, 48f).dp
