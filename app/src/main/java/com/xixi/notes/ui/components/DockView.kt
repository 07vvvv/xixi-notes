package com.xixi.notes.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xixi.notes.R
import com.xixi.notes.ui.util.GentleEasing
import kotlin.math.cos
import kotlin.math.max

/** Dock 高度 */
val DockHeight: Dp = 64.dp

/** 每个标签占位宽度 */
private val TabWidth = 64.dp

/** 图标尺寸 */
private val GlyphSize = 22.dp

/** 4 个标签（文字统一走 strings.xml） */
enum class DockTab(@param:StringRes val labelRes: Int) {
    BOARD(R.string.dock_board),
    STATS(R.string.dock_stats),
    ARCHIVE(R.string.dock_archive),
    SETTINGS(R.string.dock_settings)
}

private fun DockTab.icon(): ImageVector = when (this) {
    DockTab.BOARD -> Icons.Default.Home
    DockTab.STATS -> Icons.Default.BarChart
    DockTab.ARCHIVE -> Icons.Default.Inventory2
    DockTab.SETTINGS -> Icons.Default.Settings
}

/**
 * 底部 Dock：4 标签，只缩放 glyph，无背景 tile。
 *
 * - 选中项下方 4dp 实心圆点，选中时显示文字
 * - 悬停 / 按压放大：`f = max(0, (1 + cos(180° * d / 3)) / 2)`
 * - `translateY(8px * f * -1) scale(1 + 1.32f - f)`
 * - 滚动时整体缩放（由 [scale] 驱动），始终可见
 */
@Composable
fun DockView(
    selected: DockTab,
    onSelect: (DockTab) -> Unit,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DockHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                // 缩放围绕底部中心，避免整体上浮
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockTab.entries.forEachIndexed { index, tab ->
            DockTabItem(
                tab = tab,
                selected = tab == selected,
                selectedIndex = selected.ordinal,
                index = index,
                liftPx = with(density) { 8.dp.toPx() },
                onClick = { onSelect(tab) }
            )
        }
    }
}

@Composable
private fun DockTabItem(
    tab: DockTab,
    selected: Boolean,
    selectedIndex: Int,
    index: Int,
    liftPx: Float,
    onClick: () -> Unit
) {
    val label = stringResource(tab.labelRes)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()

    // 距离选中项越近，f 越大：f = max(0, (1 + cos(180° * d / 3)) / 2)
    val distance = kotlin.math.abs(index - selectedIndex).toFloat()
    val base = max(0f, (1f + cos(Math.PI.toFloat() * distance / 3f)) / 2f)
    // 悬停 / 按压进一步放大当前项
    val activeBoost = if (pressed) 1f else if (hovered) 0.6f else 0f
    val targetF = max(base, activeBoost)

    val f by animateFloatAsState(
        targetValue = targetF,
        animationSpec = tween(durationMillis = 220, easing = GentleEasing),
        label = "dock_f_$index"
    )

    val glyphScale = 1f + 1.32f * f - f
    val translateY = 8f * f * -1f

    Column(
        modifier = Modifier
            .width(TabWidth)
            .clickableNoRipple(interactionSource = interactionSource, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(GlyphSize + 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tab.icon(),
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(GlyphSize)
                    .graphicsLayer {
                        scaleX = glyphScale
                        scaleY = glyphScale
                        translationY = translateY * (liftPx / 8f)
                    }
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // 选中项下方 4dp 实心圆点
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else androidx.compose.ui.graphics.Color.Transparent
                )
        )

        // 选中时显示文字
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(animationSpec = tween(180, easing = GentleEasing)),
            exit = fadeOut(animationSpec = tween(140, easing = GentleEasing))
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
