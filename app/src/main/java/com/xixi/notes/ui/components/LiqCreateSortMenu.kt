package com.xixi.notes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.xixi.notes.R
import com.xixi.notes.ui.util.GentleEasing
import com.xixi.notes.ui.util.LIQ_EXPAND_DURATION
import kotlinx.coroutines.delay

/** 闭合按钮直径 */
val LiqButtonSize: Dp = 46.dp

/** 展开面板尺寸 */
val LiqPanelWidth: Dp = 212.dp
val LiqPanelHeight: Dp = 190.dp

/** 面板圆角 */
private val LiqPanelRadius: Dp = 26.dp

/** 面板与按钮之间的间距 */
private val LiqPanelGap: Dp = 6.dp

/**
 * 排序按钮（46dp 圆形）。
 *
 * 只负责显示图标与上报自己在窗口中的位置；**面板不在这里渲染**。
 *
 * 关键原因：这个按钮位于高度固定 56dp 的顶部栏 Row 内，`Modifier.offset` 不参与
 * 父级尺寸计算，若把 212×190 的面板放在这里，它会先被 56dp 的父级约束夹扁、
 * 再被顶部栏裁剪，结果只露出半行文字。所以面板必须由 [LiqSortMenuOverlay]
 * 在根部全屏层渲染。
 *
 * @param onAnchor 上报按钮在窗口坐标系中的边界，供面板对齐
 */
@Composable
fun LiqSortButton(
    open: Boolean,
    onToggle: () -> Unit,
    onAnchor: (buttonLeft: Float, buttonTop: Float, buttonWidth: Float, buttonHeight: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // 按压先缩小 0.94，持续 120ms
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 120, easing = GentleEasing),
        label = "liq_press"
    )

    LaunchedEffect(open) {
        if (open) {
            pressed = true
            delay(120)
            pressed = false
        }
    }

    Box(
        modifier = modifier
            .size(LiqButtonSize)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                onAnchor(bounds.left, bounds.top, bounds.width, bounds.height)
            }
            .clickableNoRipple { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Sort,
            contentDescription = stringResource(R.string.cd_sort),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 排序菜单覆盖层：在根部全屏层渲染，绝对定位贴在排序按钮右下方。
 *
 * - 212dp × 190dp、圆角 26dp，liq-create 风格原地展开
 * - 展开延迟 95ms、480ms 同曲线（无 overshoot）
 * - 菜单项延迟 70 + i * 38ms
 * - 选中项左侧彩色竖条 + 文字加粗
 * - 透明全屏点击层：点击外部（含列表、空白、Dock）自动收起
 * - 空间不足（靠近屏幕底部）时自动向上展开，保证完整可见
 *
 * @param anchorX 排序按钮右边缘的窗口 x 坐标
 * @param anchorY 排序按钮底边的窗口 y 坐标
 */
@Composable
fun LiqSortMenuOverlay(
    anchorX: Float,
    anchorY: Float,
    open: Boolean,
    options: List<SortOptionUi>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!open) return

    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    // 展开进度：延迟 95ms 后开始，480ms 同曲线
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = LIQ_EXPAND_DURATION,
            delayMillis = 95,
            easing = GentleEasing
        ),
        label = "liq_progress"
    )

    val panelWidthPx = with(density) { LiqPanelWidth.toPx() }
    val panelHeightPx = with(density) { LiqPanelHeight.toPx() }
    val gapPx = with(density) { LiqPanelGap.toPx() }
    val marginPx = with(density) { 12.dp.toPx() }

    // 水平：右边缘与按钮右边缘对齐，并夹在屏幕内
    val rawX = anchorX - panelWidthPx
    val panelX = rawX.coerceIn(marginPx, (screenWidthPx - panelWidthPx - marginPx).coerceAtLeast(marginPx))

    // 垂直：优先向下展开（按钮底边 + 间距），放不下则向上展开
    val belowY = anchorY + gapPx
    val aboveY = anchorY - panelHeightPx - gapPx
    val panelY = if (belowY + panelHeightPx > screenHeightPx - marginPx) aboveY else belowY

    Box(modifier = modifier.fillMaxSize().zIndex(1f)) {
        // 透明全屏点击层：点击外部收起菜单
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickableNoRipple { onDismiss() }
        )

        // 面板本体（位置与尺寸由外层计算，不受顶部栏 56dp 约束）
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = panelX.toInt(),
                        y = panelY.coerceAtLeast(marginPx).toInt()
                    )
                }
                .width(LiqPanelWidth * progress)
                .height(LiqPanelHeight * progress)
                .clip(RoundedCornerShape(LiqPanelRadius * progress))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(LiqPanelRadius * progress)
                )
                // 吃掉落在面板上的指针事件，避免误触发底层的"点击外部收起"
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.Center
            ) {
                options.forEachIndexed { index, option ->
                    // 菜单项延迟 70 + i * 38ms
                    var itemVisible by remember { mutableStateOf(false) }
                    val itemProgress by animateFloatAsState(
                        targetValue = if (itemVisible) 1f else 0f,
                        animationSpec = tween(durationMillis = 220, easing = GentleEasing),
                        label = "liq_item_$index"
                    )
                    LaunchedEffect(progress) {
                        if (progress > 0.9f) {
                            delay(70L + index * 38L)
                            itemVisible = true
                        } else if (progress < 0.2f) {
                            itemVisible = false
                        }
                    }

                    val textColor by animateColorAsState(
                        targetValue = if (option.selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(durationMillis = 220),
                        label = "liq_item_color_$index"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickableNoRipple { onSelect(option.id) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 选中项左侧彩色竖条
                        Box(
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .width(3.dp)
                                .height(22.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (option.selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                        )
                        Text(
                            text = option.label,
                            color = textColor.copy(alpha = 0.35f + 0.65f * itemProgress),
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp,
                            fontWeight = if (option.selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 排序面板中的一个选项 */
data class SortOptionUi(
    val id: String,
    val label: String,
    val selected: Boolean
)
