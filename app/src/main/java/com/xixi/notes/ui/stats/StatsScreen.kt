package com.xixi.notes.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xixi.notes.R
import com.xixi.notes.di.LocalAppContainer
import com.xixi.notes.ui.board.Quadrant
import com.xixi.notes.ui.components.clickableNoRipple
import com.xixi.notes.ui.components.colorOf
import com.xixi.notes.ui.theme.Spacing
import com.xixi.notes.ui.theme.XixiElevation
import com.xixi.notes.ui.theme.XixiLargeCardShape
import com.xixi.notes.ui.theme.XixiItemShape
import com.xixi.notes.ui.theme.XixiTextStyles
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.GentleEasing

/**
 * 统计页。
 *
 * 布局（自上而下）：
 * 1. 标题栏
 * 2. **顶部大卡片**：任务总数（核心数字，最大最粗）
 * 3. **2 × 2 网格**：四象限数量，各自带象限色，可点击跳转主屏筛选
 * 4. 底部卡片：本周完成 / 总完成（跳归档页）、逾期（跳主屏逾期筛选）
 *
 * 所有卡片的点击逻辑与数据来源保持不变（统计计算在 StatsViewModel，本文件只做展示）。
 */
@Composable
fun StatsScreen(
    onQuadrantClick: (Quadrant) -> Unit,
    onCompletedClick: () -> Unit,
    onOverdueClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.Factory(LocalAppContainer.current.taskRepository)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XixiTheme.colors.background)
            .systemBarsPadding()
    ) {
        // 顶部栏：只显示标题
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = Spacing.xl),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = stringResource(R.string.stats_title),
                color = XixiTheme.colors.textPrimary,
                style = XixiTextStyles.pageTitle
            )
        }

        if (state.isEmpty) {
            EmptyStats()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenH),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // ------------------------------------------------ 顶部大卡片：核心数字
                TotalTasksCard(total = state.totalTasks)

                // ------------------------------------------------------ 2 × 2 四象限网格
                Quadrant.ordered.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        pair.forEach { quadrant ->
                            QuadrantCountCard(
                                quadrant = quadrant,
                                count = state.counts[quadrant] ?: 0,
                                onClick = { onQuadrantClick(quadrant) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ------------------------------------------- 本周完成 / 总完成 / 逾期
                StatLineCard(
                    label = stringResource(R.string.stats_week_completed),
                    value = state.weekCompleted,
                    accent = XixiTheme.colors.accent,
                    onClick = onCompletedClick
                )
                StatLineCard(
                    label = stringResource(R.string.stats_total_completed),
                    value = state.totalCompleted,
                    accent = XixiTheme.colors.accent,
                    onClick = onCompletedClick
                )
                // 逾期用危险语义色
                StatLineCard(
                    label = stringResource(R.string.stats_overdue),
                    value = state.overdue,
                    accent = XixiTheme.colors.danger,
                    onClick = onOverdueClick
                )
            }
        }
    }
}

/**
 * 顶部大卡片：任务总数（核心数字）。
 *
 * 不可点击（没有对应的目标页面），因此不加点击反馈，只作为视觉焦点存在。
 */
@Composable
private fun TotalTasksCard(
    total: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(XixiElevation.cardStrong, XixiLargeCardShape)
            .clip(XixiLargeCardShape)
            .background(XixiTheme.colors.card)
            .padding(Spacing.largeCardPadding)
    ) {
        Text(
            text = stringResource(R.string.stats_total_tasks),
            color = XixiTheme.colors.textSecondary,
            style = XixiTextStyles.caption,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        RollingNumber(
            value = total,
            color = XixiTheme.colors.textPrimary,
            style = XixiTextStyles.statNumber
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.stats_tap_hint),
            color = XixiTheme.colors.textTertiary,
            style = XixiTextStyles.minor
        )
    }
}

/** 点击缩放反馈包装：按下缩到 0.97，150ms 回到 1.0 */
@Composable
private fun ClickScaleBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "click_scale"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickableNoRipple(interactionSource = interactionSource, onClick = onClick)
    ) {
        content()
    }
}

/** 单个象限卡片：象限色圆点 + 名称 + 数量，点击按象限筛选主屏 */
@Composable
private fun QuadrantCountCard(
    quadrant: Quadrant,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = XixiTheme.quadrant.colorOf(quadrant)
    ClickScaleBox(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(XixiElevation.card, XixiItemShape)
                .clip(XixiItemShape)
                .background(XixiTheme.colors.card)
                .padding(Spacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = stringResource(quadrant.titleRes),
                    color = XixiTheme.colors.textSecondary,
                    style = XixiTextStyles.minor,
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            RollingNumber(
                value = count,
                color = color,
                style = XixiTextStyles.statNumberSmall
            )
        }
    }
}

/** 单行统计卡片（可点击：完成类跳归档，逾期跳主屏筛选） */
@Composable
private fun StatLineCard(
    label: String,
    value: Int,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ClickScaleBox(onClick = onClick, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(XixiElevation.card, XixiItemShape)
                .clip(XixiItemShape)
                .background(XixiTheme.colors.card)
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = XixiTheme.colors.textPrimary,
                style = XixiTextStyles.rowTitle,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            RollingNumber(
                value = value,
                color = accent,
                style = XixiTextStyles.statNumberLine
            )
        }
    }
}

/** 数字滚动 400ms（样式由调用方通过 [style] 指定，保证统计数字层级统一） */
@Composable
private fun RollingNumber(
    value: Int,
    color: Color,
    style: TextStyle
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 400, easing = GentleEasing),
        label = "stat_number"
    )
    Text(
        text = animated.toString(),
        color = color,
        style = style
    )
}

/** 空数据状态 */
@Composable
private fun EmptyStats() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.huge),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.stats_empty),
                color = XixiTheme.colors.textPrimary,
                style = XixiTextStyles.subtitle,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.stats_empty_hint),
                color = XixiTheme.colors.textSecondary,
                style = XixiTextStyles.caption,
                textAlign = TextAlign.Center
            )
        }
    }
}
