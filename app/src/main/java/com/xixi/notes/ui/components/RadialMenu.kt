package com.xixi.notes.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.xixi.notes.R
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

/** FAB 直径 */
val FabSize: Dp = 56.dp

/** 象限按钮直径 */
private val SECTOR_BUTTON = 48.dp

/** 展开半径上限 */
private val RADIUS_MAX = 100.dp

/** 展开半径下限 */
private val RADIUS_MIN = 70.dp

/** 拖动阈值：小于该距离视为 tap */
private val TAP_THRESHOLD = 14.dp

/** 相邻按钮的展开延迟（依次弹开） */
private const val STEP_DELAY_MS = 40L

/** 遮罩透明度 */
private const val SCRIM_ALPHA = 0.30f

/**
 * 四个按钮的角度（屏幕坐标系 y 向下，上半圆 180°）。
 *
 * 0°（正右）轻急 / 45°（右上）重急 / 135°（左上）重缓 / 180°（正左）轻缓
 */
private data class SectorSpec(val quadrant: Quadrant, val degrees: Float)

private val SECTORS = listOf(
    SectorSpec(Quadrant.URGENT_ONLY, 0f),
    SectorSpec(Quadrant.IMPORTANT_URGENT, 45f),
    SectorSpec(Quadrant.IMPORTANT_ONLY, 135f),
    SectorSpec(Quadrant.NEITHER, 180f)
)

/** 象限 -> 固定颜色 */
fun QuadrantColors.colorOf(quadrant: Quadrant): Color = when (quadrant) {
    Quadrant.IMPORTANT_URGENT -> importantUrgent
    Quadrant.URGENT_ONLY -> urgentOnly
    Quadrant.IMPORTANT_ONLY -> importantOnly
    Quadrant.NEITHER -> neither
}

/**
 * 角度归入最近的扇区（上半圆 180°，y 轴向上）。
 *
 * - 轻急：[0°, 22.5°]（中心 0°）
 * - 重急：(22.5°, 67.5°)（中心 45°）
 * - 重缓：[67.5°, 112.5°)（中心 135°）
 * - 轻缓：[112.5°, 180°]（中心 180°）
 *
 * 入参是**屏幕坐标差**（y 向下），内部先取负还原成数学坐标。
 */
fun nearestFanSector(dx: Float, dy: Float): Quadrant {
    val angle = Math.toDegrees(atan2((-dy).toDouble(), dx.toDouble())).toFloat()
    // 归一到 [0°, 180°]：上半圆
    val normalized = when {
        angle < 0f -> angle + 180f
        angle > 180f -> angle - 180f
        else -> angle
    }
    return when {
        normalized <= 22.5f -> Quadrant.URGENT_ONLY
        normalized < 67.5f -> Quadrant.IMPORTANT_URGENT
        normalized < 112.5f -> Quadrant.IMPORTANT_ONLY
        else -> Quadrant.NEITHER
    }
}

/**
 * FAB + 上半圆 Radial Menu。
 *
 * 层级（自下而上）：拖动层 -> 遮罩(30% 黑) -> 四个象限按钮 -> FAB(zIndex 1f)。
 *
 * - FAB 位于屏幕底部水平居中，位置由父级测量后通过 [centerX] / [centerY] 传入
 * - 展开时 FAB 图标旋转 45°（+ 变 ×）
 * - 四个按钮依次弹开（每个延迟 40ms，spring 带过冲），从 FAB 中心 scale 0 弹到 scale 1
 * - 支持点击选择与拖动选择；点击遮罩收起
 */
@Composable
fun RadialMenuHost(
    centerX: Float,
    centerY: Float,
    open: Boolean,
    onToggle: () -> Unit,
    onSelect: (Quadrant) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    val currentSelect by rememberUpdatedState(onSelect)
    val currentDismiss by rememberUpdatedState(onDismiss)

    // FAB 图标旋转：展开 45° 变 ×
    val fabRotation by animateFloatAsState(
        targetValue = if (open) 45f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "fab_rotation"
    )

    // 半径：受屏幕宽度与顶部空间限制，空间不足时收缩（最小 70dp）
    val radiusPx = if (screenWidthPx <= 0f || centerY <= 0f) {
        with(density) { RADIUS_MIN.toPx() }
    } else {
        val maxPx = with(density) { RADIUS_MAX.toPx() }
        val minPx = with(density) { RADIUS_MIN.toPx() }
        val halfButton = with(density) { SECTOR_BUTTON.toPx() } / 2f
        val sideMargin = with(density) { 12.dp.toPx() }
        // 正左 / 正右两个按钮需要横向空间
        val horizontalRoom = screenWidthPx / 2f - sideMargin - halfButton
        // 左上 / 右上两个按钮需要顶部空间
        val verticalRoom = centerY - halfButton - with(density) { 24.dp.toPx() }
        min(maxPx, min(horizontalRoom, verticalRoom)).coerceAtLeast(minPx)
    }

    Box(modifier = modifier.fillMaxSize().zIndex(2f)) {

        // ------------------------------------------------- 拖动选择手势层
        if (open) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0.2f)
                    .pointerInput(centerX, centerY) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var lastPosition = down.position
                            var up = false
                            while (!up) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                lastPosition = change.position
                                if (change.changedToUp() || !change.pressed) {
                                    up = true
                                }
                            }
                            val dx = lastPosition.x - centerX
                            val dy = lastPosition.y - centerY
                            if (hypot(dx, dy) < TAP_THRESHOLD.toPx()) {
                                // 拖动距离小于阈值视为 tap：关闭
                                currentDismiss()
                            } else {
                                currentSelect(nearestFanSector(dx, dy))
                            }
                        }
                    }
            )
        }

        // ------------------------------------------------------------ 遮罩
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(animationSpec = tween(200, easing = GentleEasing)),
            exit = fadeOut(animationSpec = tween(200, easing = GentleEasing)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0.5f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                    // 点击遮罩收起；同时拦截点击避免穿透到底层列表
                    .clickableNoRipple { currentDismiss() }
            )
        }

        // -------------------------------------------------- 四个象限按钮
        val quadrantColors = XixiTheme.quadrant
        SECTORS.forEachIndexed { index, sector ->
            // 依次弹开：每个延迟 40ms
            var started by remember { mutableStateOf(false) }
            LaunchedEffect(open) {
                if (open) {
                    delay(index * STEP_DELAY_MS)
                    started = true
                } else {
                    started = false
                }
            }

            val progress by animateFloatAsState(
                targetValue = if (open && started) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                label = "sector_$index"
            )

            if (progress > 0.01f) {
                val angleRad = Math.toRadians(sector.degrees.toDouble())
                val dx = (cos(angleRad) * radiusPx).toFloat()
                // 屏幕坐标系 y 向下，向上弹开取负
                val dy = (-sin(angleRad) * radiusPx).toFloat()

                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { (centerX + dx).toDp() } - SECTOR_BUTTON / 2,
                            y = with(density) { (centerY + dy).toDp() } - SECTOR_BUTTON / 2
                        )
                        .size(SECTOR_BUTTON)
                        .zIndex(0.8f)
                        .graphicsLayer {
                            // 从 FAB 中心 (0,0) 与 scale 0 弹到目标位置与 scale 1
                            translationX = dx * (1f - progress)
                            translationY = dy * (1f - progress)
                            scaleX = progress
                            scaleY = progress
                            alpha = progress.coerceIn(0f, 1f)
                        }
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(quadrantColors.colorOf(sector.quadrant))
                        .clickableNoRipple { currentSelect(sector.quadrant) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(sector.quadrant.shortTitleRes),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        // ------------------------------------------------------------- FAB
        Box(
            modifier = Modifier
                .offset(
                    x = with(density) { centerX.toDp() } - FabSize / 2,
                    y = with(density) { centerY.toDp() } - FabSize / 2
                )
                .size(FabSize)
                .zIndex(1f)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickableNoRipple { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(
                    if (open) R.string.cd_radial_close else R.string.cd_new_task
                ),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(fabRotation)
            )
        }
    }
}

/**
 * 诊断辅助：FAB 到屏幕顶部的距离是否足够放下「半径 + 按钮高度」。
 */
internal fun hasVerticalRoom(fabCenterY: Float, radiusPx: Float, buttonPx: Float): Boolean =
    fabCenterY - radiusPx - buttonPx / 2f > 0f
