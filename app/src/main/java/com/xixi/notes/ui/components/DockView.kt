package com.xixi.notes.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xixi.notes.R
import com.xixi.notes.ui.theme.Spacing
import com.xixi.notes.ui.theme.XixiRadius
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.GentleEasing
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max

/** Dock 外层卡片高度（名字与可见性保持不变） */
val DockHeight: Dp = 56.dp

/** Dock 卡片内边距（留白） */
private val DockPaddingH = 8.dp
private val DockPaddingV = 6.dp

/** 每个标签占位宽度 */
private val TabWidth = 64.dp

/** 图标尺寸 */
private val GlyphSize = 22.dp

/** 选中项下方圆点尺寸 */
private val DotSize = 5.dp

/** Dock 卡片圆角 */
private val DockShape = RoundedCornerShape(XixiRadius.largeCard)

/** 底部系统栏 ∪ 输入法 的 insets（Dock 自身负责避让，外部不要再加内边距） */
private val DockBottomInsets = WindowInsets.navigationBars
    .only(WindowInsetsSides.Bottom)
    .union(WindowInsets.ime)

/** 4 个标签（文字统一走 strings.xml，仅用于无障碍朗读，界面不再显示文字） */
enum class DockTab(@param:StringRes val labelRes: Int) {
    BOARD(R.string.dock_board),
    STATS(R.string.dock_stats),
    ARCHIVE(R.string.dock_archive),
    SETTINGS(R.string.dock_settings)
}

/** Dock 标签图标 */
private fun DockTab.icon(): ImageVector = when (this) {
    DockTab.BOARD -> Icons.Default.Home
    DockTab.STATS -> Icons.Default.BarChart
    DockTab.ARCHIVE -> Icons.Default.Inventory2
    DockTab.SETTINGS -> Icons.Default.Settings
}

/**
 * 底部 Dock：4 个图标 + 选中圆点，**不显示文字标签**。
 *
 * 视觉实现要点（Bug 修复相关）：
 * - 外层不再使用 Material3 `Surface`。`Surface` 会引入 tonal overlay / contentColor /
 *   隐式交互覆盖等行为，是"点击闪黑"的常见来源；这里改为纯 [Box] + 纯色底 + 柔和阴影，
 *   圆角由 [clip] 控制，行为完全可控。
 * - 深色背景只画在 Dock 卡片自身上（不再由外部绘制整宽整高的背景条），
 *   避免切换标签时那一块不透明深色区域闪出。
 * - 底部系统栏 / 输入法避让由 Dock 自己通过 [DockBottomInsets] 处理，
 *   外部（MainScaffold）不要再叠加 `windowInsetsPadding`，否则 insets 会被算两次。
 *
 * 动效（未改动）：
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
    // 深色底上阴影更弱，避免糊成一团
    val shadowElevationPx = with(density) {
        (if (XixiTheme.colors.isDark) 3.dp else 5.dp).toPx()
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(DockBottomInsets)
            .padding(horizontal = Spacing.lg)
            .padding(bottom = Spacing.sm)
            .height(DockHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                // 缩放围绕底部中心，避免整体上浮
                transformOrigin = TransformOrigin(0.5f, 1f)
                shadowElevation = shadowElevationPx
                shape = DockShape
                clip = false
            }
            .clip(DockShape)
            .background(XixiTheme.colors.card),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DockPaddingH, vertical = DockPaddingV),
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
    // 文字不再显示，但仍用于无障碍朗读
    val label = stringResource(tab.labelRes)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()

    // 距离选中项越近，f 越大：f = max(0, (1 + cos(180° * d / 3)) / 2)
    val distance = abs(index - selectedIndex).toFloat()
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
    // 选中项弹性缩放：1.0 -> 1.15 -> 1.0（弹性回弹）
    val selectScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dock_select_scale_$index"
    )

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
                // 选中态使用全 App 统一强调色，未选中为次要文字色
                tint = if (selected) XixiTheme.colors.accent else XixiTheme.colors.textSecondary,
                modifier = Modifier
                    .size(GlyphSize)
                    .graphicsLayer {
                        scaleX = glyphScale * selectScale
                        scaleY = glyphScale * selectScale
                        translationY = translateY * (liftPx / 8f)
                    }
            )
        }

        // 选中项下方实心圆点（未选中透明，保持布局稳定）
        Box(
            modifier = Modifier
                .padding(top = Spacing.xs)
                .size(DotSize)
                .clip(CircleShape)
                .background(if (selected) XixiTheme.colors.accent else Color.Transparent)
        )
    }
}
