package com.xixi.notes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xixi.notes.R
import com.xixi.notes.data.local.TaskEntity
import com.xixi.notes.ui.board.Quadrant
import com.xixi.notes.ui.theme.Spacing
import com.xixi.notes.ui.theme.XixiElevation
import com.xixi.notes.ui.theme.XixiItemShape
import com.xixi.notes.ui.theme.XixiPillShape
import com.xixi.notes.ui.theme.XixiTextStyles
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.formatShortDateTime
import com.xixi.notes.ui.util.isOverdue
import kotlinx.coroutines.delay

/** 任务行最小高度（单行：标题 + 右侧截止时间 + 叉号） */
val TaskRowHeight = 60.dp

/** 任务行卡片圆角（条目卡片 16dp） */
private val RowShape = XixiItemShape

/**
 * 任务行。
 *
 * - 左：完成圆圈 24dp —— 点击完成 / 取消完成；完成态为实心 + 白色对勾
 * - 中：标题单行省略；完成时删除线 + 整行 40% 透明；有图片时标题右侧显示 Image 图标
 * - 右：截止时间（MM-dd HH:mm）在叉号左侧，过期红色，未设置不显示
 * - 右：叉号 24dp —— 点击删除（非重要任务直接删除 + 撤销，重要任务原地 Inline Confirm）
 * - 长按 500ms 触发 [onLongPress]（保留：作为删除的快捷入口）
 *
 * 视觉：卡片底色 + 柔和阴影 + 16dp 圆角；辅助信息用三级文字色；
 * 逾期用危险色；完成态保持独立的「完成绿」，不并入统一强调色。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(
    task: TaskEntity,
    quadrant: Quadrant,
    highlightAlpha: Float,
    onToggleCheck: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 每分钟刷新一次当前时间：到期后 1 分钟内自动变为过期红色（LaunchedEffect + delay）
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            now = System.currentTimeMillis()
        }
    }

    val quadrantColor = XixiTheme.quadrant.colorOf(quadrant)
    val due = task.dueDate
    val overdue = due != null && isOverdue(due, now)

    // 完成时整行 40% 透明（含标题、截止时间与两个按钮）
    val contentAlpha = if (task.isCheckedOff) 0.4f else 1f

    // 高亮闪烁：统一强调色的 20% 透明 -> 0%
    val highlightColor = XixiTheme.colors.accent.copy(alpha = 0.20f * highlightAlpha)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = 3.dp)
            .shadow(XixiElevation.card, RowShape)
            .clip(RowShape)
            .background(XixiTheme.colors.card)
            .background(highlightColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TaskRowHeight)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongPress
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左：完成圆圈
            CompleteCircle(
                checked = task.isCheckedOff,
                color = quadrantColor,
                onToggle = onToggleCheck
            )

            Spacer(modifier = Modifier.width(Spacing.md))

            // 中：标题（单行省略）+ 可选图片角标
            Row(
                modifier = Modifier
                    .weight(1f)
                    .alpha(contentAlpha),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    color = XixiTheme.colors.textPrimary,
                    style = XixiTextStyles.rowTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCheckedOff) TextDecoration.LineThrough
                    else TextDecoration.None,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (task.imagePaths.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = stringResource(R.string.cd_has_image),
                        tint = XixiTheme.colors.textTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // 右：截止时间（叉号左侧），未设置时不占位
            if (due != null) {
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = formatShortDateTime(due),
                    // 已过期用危险色，其余为辅助信息色
                    color = if (overdue) XixiTheme.colors.danger
                    else XixiTheme.colors.textTertiary,
                    style = XixiTextStyles.minor,
                    maxLines = 1,
                    modifier = Modifier.alpha(contentAlpha)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            // 右：删除叉号
            DeleteButton(
                onDelete = onDelete,
                modifier = Modifier.alpha(contentAlpha)
            )
        }
    }
}

/**
 * 完成圆圈 24dp：未完成是细线圆环，完成后为象限色实心 + 白色对勾。
 *
 * @param color 完成后的实心填充色（取任务所属象限色）
 */
@Composable
fun CompleteCircle(
    checked: Boolean,
    color: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.9f,
        animationSpec = tween(durationMillis = 220),
        label = "complete_scale"
    )
    val fill by animateColorAsState(
        targetValue = if (checked) color else Color.Transparent,
        animationSpec = tween(durationMillis = 250),
        label = "complete_fill"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) color else XixiTheme.colors.textTertiary,
        animationSpec = tween(durationMillis = 250),
        label = "complete_border"
    )
    val tickAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "complete_tick"
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(fill)
            .border(width = 1.5.dp, color = borderColor, shape = CircleShape)
            .clickableNoRipple(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (tickAlpha > 0.01f) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(
                    if (checked) R.string.cd_uncomplete_task else R.string.cd_complete_task
                ),
                // 对勾用专用墨绿，保证在象限色实心圆上的对比度
                tint = XixiTheme.colors.onCheck.copy(alpha = tickAlpha),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** 删除叉号 24dp：点击删除（非重要任务直接删，重要任务由父级弹 Inline Confirm） */
@Composable
fun DeleteButton(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickableNoRipple(onClick = onDelete),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.cd_delete_task),
            tint = XixiTheme.colors.textTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** 分组头部卡片圆角（条目卡片 16dp） */
private val GroupHeaderShape = XixiItemShape

/** 分组头部卡片高度 */
private val GroupHeaderHeight = 52.dp

/**
 * 分组头部：圆角小卡片（分组名 + 数量徽章 + 折叠箭头）。
 *
 * 视觉：象限色实心圆点标记分类，标题主色加粗，数量徽章用胶囊 + 象限色淡底，
 * 箭头为次要色；卡片本身使用与任务行一致的底色与柔和阴影，形成清晰的分区节奏。
 */
@Composable
fun GroupHeaderRow(
    title: String,
    count: Int,
    color: Color,
    folded: Boolean,
    onToggleFold: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (folded) -90f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "group_arrow"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            .height(GroupHeaderHeight)
            .shadow(XixiElevation.card, GroupHeaderShape)
            .clip(GroupHeaderShape)
            .background(XixiTheme.colors.card)
            .clickableNoRipple(onClick = onToggleFold)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 象限色实心圆点
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            text = title,
            color = XixiTheme.colors.textPrimary,
            style = XixiTextStyles.rowTitle,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        // 数量徽章
        Box(
            modifier = Modifier
                .clip(XixiPillShape)
                .background(color.copy(alpha = 0.18f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                color = color,
                style = XixiTextStyles.micro,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        // 折叠箭头：折叠时旋转 -90°
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = stringResource(
                if (folded) R.string.cd_expand_group else R.string.cd_collapse_group
            ),
            tint = XixiTheme.colors.textSecondary,
            modifier = Modifier
                .size(20.dp)
                .rotate(arrowRotation)
        )
    }
}

/** 空分类提示：分组展开但没有任何任务时显示在头部下方 */
@Composable
fun EmptyGroupHint(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Text(
            text = stringResource(R.string.group_empty_hint),
            color = XixiTheme.colors.textTertiary,
            style = XixiTextStyles.caption
        )
    }
}
