package com.xixi.notes.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xixi.notes.di.RadialMenuState
import com.xixi.notes.ui.board.Quadrant
import com.xixi.notes.ui.theme.QuadrantColors
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.GentleEasing
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** 扇区半径 */
private val SECTOR_RADIUS = 80.dp

/** 最小扇区半径（贴边时收缩，不偏移中心） */
private val SECTOR_RADIUS_MIN = 70.dp

/** 扇区直径 */
private val SECTOR_SIZE = 48.dp

/** 拖动阈值：小于该距离视为 tap */
private val TAP_THRESHOLD = 14.dp

/** 过渡延迟步长 */
private const val STEP_DELAY_MS = 36L

/**
 * 四个方向（屏幕坐标系 y 向下）：
 * 右上 -45° / 右下 45° / 左下 135° / 左上 -135°
 */
private val SECTOR_ANGLES = listOf(-45f, 45f, 135f, -135f)

/**
 * 角度归入扇区（边界角度归入下界扇区）。
 *
 * - 右上：[-90°, 0°)
 * - 右下：[0°, 90°)
 * - 左下：[90°, 180°]
 * - 左上：[-180°, -90°)，-180° 与 180° 是同一方向，归入左上
 */
fun nearestByAngle(dx: Float, dy: Float): Quadrant {
    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    return when {
        angle >= -90f && angle < 0f -> Quadrant.IMPORTANT_URGENT
        angle >= 0f && angle < 90f -> Quadrant.URGENT_ONLY
        angle >= 90f -> Quadrant.IMPORTANT_ONLY
        else -> Quadrant.NEITHER
    }
}

/** 象限 -> 固定颜色 */
fun QuadrantColors.colorOf(quadrant: Quadrant): Color = when (quadrant) {
    Quadrant.IMPORTANT_URGENT -> importantUrgent
    Quadrant.URGENT_ONLY -> urgentOnly
    Quadrant.IMPORTANT_ONLY -> importantOnly
    Quadrant.NEITHER -> neither
}

/**
 * Radial Menu 遮罩 + 菜单（Canvas 自绘，整圆 360°）。
 *
 * - 遮罩覆盖全屏 30% 黑，拦截所有点击
 * - 扇区：彩色圆点 8dp + 短标签（rememberTextMeasurer + drawText）
 * - 扇区过渡延迟 i * 36ms
 */
@Composable
fun RadialMenuOverlay(
    state: RadialMenuState,
    onSelect: (Quadrant) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // 保持回调最新，避免 pointerInput 因重组重启
    val currentSelect by rememberUpdatedState(onSelect)
    val currentDismiss by rememberUpdatedState(onDismiss)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = GentleEasing),
        label = "radial_scrim"
    )

    val quadrants = Quadrant.ordered
    val labelStyle = TextStyle(
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFFFAFAFA)
    )
    val labelTexts = quadrants.map { stringResource(it.shortTitleRes) }
    val quadrantColors = XixiTheme.quadrant

    // 预测量中文标签，绘制时复用
    val labels: List<TextLayoutResult> = remember(textMeasurer, labelTexts) {
        labelTexts.map { text ->
            textMeasurer.measure(text = text, style = labelStyle, maxLines = 1)
        }
    }

    // 每个扇区独立的展开进度（延迟 i * 36ms）
    val sectorProgress = quadrants.indices.map { index ->
        var started by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(index * STEP_DELAY_MS)
            started = true
        }
        animateFloatAsState(
            targetValue = if (started) 1f else 0f,
            animationSpec = tween(durationMillis = 240, easing = GentleEasing),
            label = "radial_sector_$index"
        ).value
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // 拦截所有点击，避免穿透到底层列表
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val centerX = state.centerX.coerceIn(0f, widthPx)
        val centerY = state.centerY.coerceIn(0f, heightPx)

        val baseRadius = with(density) { SECTOR_RADIUS.toPx() }
        val minRadius = with(density) { SECTOR_RADIUS_MIN.toPx() }
        val halfSector = with(density) { SECTOR_SIZE.toPx() } / 2f
        // 边界安全：只缩小半径，不偏移中心
        val edgeRoom = min(
            min(centerX, widthPx - centerX),
            min(centerY, heightPx - centerY)
        ) + halfSector
        val radius = min(baseRadius, edgeRoom).coerceAtLeast(minRadius)

        val dotRadiusPx = with(density) { 4.dp.toPx() }
        val strokePx = with(density) { 1.5.dp.toPx() }
        val tapThresholdPx = with(density) { TAP_THRESHOLD.toPx() }
        val labelGapPx = with(density) { 2.dp.toPx() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // 全屏遮罩 30% 黑
            drawRect(
                color = Color.Black.copy(alpha = 0.30f * scrimAlpha),
                size = Size(size.width, size.height)
            )

            quadrants.forEachIndexed { index, quadrant ->
                val progress = sectorProgress[index]
                if (progress <= 0.01f) return@forEachIndexed

                val angleRad = Math.toRadians(SECTOR_ANGLES[index].toDouble())
                val cx = centerX + (radius * cos(angleRad)).toFloat()
                val cy = centerY + (radius * sin(angleRad)).toFloat()
                val half = halfSector * progress
                val color = quadrantColors.colorOf(quadrant)

                // 扇区圆底
                drawCircle(
                    color = color.copy(alpha = 0.18f),
                    radius = half,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = color.copy(alpha = 0.55f),
                    radius = half,
                    center = Offset(cx, cy),
                    style = Stroke(width = strokePx)
                )

                // 彩色圆点 8dp
                drawCircle(
                    color = color,
                    radius = dotRadiusPx * progress,
                    center = Offset(cx, cy - half * 0.32f)
                )

                // 短标签
                val layout = labels[index]
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = cx - layout.size.width / 2f,
                        y = cy - layout.size.height / 2f + dotRadiusPx + labelGapPx
                    ),
                    alpha = progress.coerceIn(0f, 1f)
                )
            }
        }

        // 手势层：拖动选择象限，拖动距离小于 14dp 视为 tap（关闭）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(centerX, centerY) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent()
                            val first = down.changes.firstOrNull() ?: continue
                            if (!first.pressed) continue

                            var lastPosition = first.position
                            var up = false
                            while (!up) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                lastPosition = change.position
                                if (change.changedToUp()) {
                                    up = true
                                } else if (!change.pressed) {
                                    up = true
                                }
                            }
                            val dx = lastPosition.x - centerX
                            val dy = lastPosition.y - centerY
                            if (hypot(dx, dy) < tapThresholdPx) {
                                currentDismiss()
                            } else {
                                currentSelect(nearestByAngle(dx, dy))
                            }
                        }
                    }
                }
        )

        // 右上角关闭按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .clickableNoRipple { currentDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭菜单",
                tint = Color(0xFFFAFAFA),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
