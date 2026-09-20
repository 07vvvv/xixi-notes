package com.xixi.notes.ui.stats

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xixi.notes.R
import com.xixi.notes.di.LocalAppContainer
import com.xixi.notes.ui.board.Quadrant
import com.xixi.notes.ui.components.colorOf
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.GentleEasing

/** 统计页：各象限数量（四卡片，不可点击）/ 本周完成 / 总完成 / 逾期数 */
@Composable
fun StatsScreen(
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
                .height(56.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = stringResource(R.string.stats_title),
                color = XixiTheme.colors.textPrimary,
                fontSize = 20.sp,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (state.isEmpty) {
            EmptyStats()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 四象限卡片（2 x 2）
                Quadrant.ordered.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { quadrant ->
                            QuadrantCountCard(
                                quadrant = quadrant,
                                count = state.counts[quadrant] ?: 0,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                StatLineCard(
                    label = stringResource(R.string.stats_week_completed),
                    value = state.weekCompleted,
                    accent = MaterialTheme.colorScheme.primary
                )
                StatLineCard(
                    label = stringResource(R.string.stats_total_completed),
                    value = state.totalCompleted,
                    accent = Color(0xFF6EE7B7)
                )
                StatLineCard(
                    label = stringResource(R.string.stats_overdue),
                    value = state.overdue,
                    accent = Color(0xFFEF4444)
                )
            }
        }
    }
}

/** 单个象限卡片：数量 + 名称，不可点击 */
@Composable
private fun QuadrantCountCard(
    quadrant: Quadrant,
    count: Int,
    modifier: Modifier = Modifier
) {
    val color = XixiTheme.quadrant.colorOf(quadrant)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(XixiTheme.colors.card)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = stringResource(quadrant.titleRes),
                color = XixiTheme.colors.textSecondary,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp,
                maxLines = 2
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        RollingNumber(
            value = count,
            color = color,
            fontSize = 28
        )
    }
}

/** 单行统计卡片 */
@Composable
private fun StatLineCard(
    label: String,
    value: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(XixiTheme.colors.card)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = XixiTheme.colors.textPrimary,
            fontSize = 14.sp,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        RollingNumber(value = value, color = accent, fontSize = 22)
    }
}

/** 数字滚动 400ms */
@Composable
private fun RollingNumber(
    value: Int,
    color: Color,
    fontSize: Int
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 400, easing = GentleEasing),
        label = "stat_number"
    )
    Text(
        text = animated.toString(),
        color = color,
        fontSize = fontSize.sp,
        letterSpacing = 0.5.sp,
        fontWeight = FontWeight.SemiBold
    )
}

/** 空数据状态 */
@Composable
private fun EmptyStats() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.stats_empty),
                color = XixiTheme.colors.textPrimary,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.stats_empty_hint),
                color = XixiTheme.colors.textSecondary,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
