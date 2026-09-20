package com.xixi.notes.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xixi.notes.ui.util.GentleEasing
import com.xixi.notes.ui.util.LIQ_EXPAND_DURATION
import kotlinx.coroutines.delay

/** 闭合按钮直径 */
val LiqButtonSize: Dp = 46.dp

/** 展开面板尺寸 */
val LiqPanelWidth: Dp = 212.dp
val LiqPanelHeight: Dp = 190.dp

/**
 * Liq-Create 排序菜单。
 *
 * - 闭合：46dp 圆形，只显示图标
 * - 展开：212dp × 190dp 面板（右下对齐按钮），圆角 26dp
 * - 按压先缩小 scale 0.94 持续 120ms，展开在 95ms 后开始
 * - width / height / radius 同曲线 480ms cubic-bezier(0.22, 1, 0.36, 1)，不 overshoot
 * - 菜单项延迟 70 + i * 38ms
 * - 选中项左侧彩色竖条 + 文字加粗
 */
@Composable
fun LiqCreateSortMenu(
    open: Boolean,
    onToggle: () -> Unit,
    options: List<SortOptionUi>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 按压先缩小 0.94，持续 120ms
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 120, easing = GentleEasing),
        label = "liq_press"
    )

    // 展开进度：延迟 95ms 后开始，480ms 同曲线
    val progress by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(
            durationMillis = LIQ_EXPAND_DURATION,
            delayMillis = if (open) 95 else 0,
            easing = GentleEasing
        ),
        label = "liq_progress"
    )

    LaunchedEffect(open) {
        if (open) {
            pressed = true
            delay(120)
            pressed = false
        }
    }

    Box(modifier = modifier) {
        // 圆形按钮（保持原位置，面板动画不影响它）
        Box(
            modifier = Modifier
                .size(LiqButtonSize)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickableNoRipple { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "排序",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }

        // 面板：右对齐按钮右边缘，向下偏移一个按钮高度
        if (progress > 0.01f) {
            SortPanel(
                progress = progress,
                options = options,
                onSelect = onSelect,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = LiqButtonSize)
            )
        }
    }
}

/** 展开后的排序面板：宽高与圆角按 [progress] 插值（同曲线，无 overshoot） */
@Composable
private fun SortPanel(
    progress: Float,
    options: List<SortOptionUi>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val width = LiqPanelWidth * progress
    val height = LiqPanelHeight * progress
    val radius = 26.dp * progress

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(radius)
            )
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
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.35f + 0.65f * itemProgress
                        ),
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

/** 排序面板中的一个选项 */
data class SortOptionUi(
    val id: String,
    val label: String,
    val selected: Boolean
)
