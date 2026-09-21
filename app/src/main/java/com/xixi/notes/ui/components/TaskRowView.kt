package com.xixi.notes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xixi.notes.R
import com.xixi.notes.data.local.TaskEntity
import com.xixi.notes.ui.board.Quadrant
import com.xixi.notes.ui.theme.Spacing
import com.xixi.notes.ui.theme.XixiElevation
import com.xixi.notes.ui.theme.XixiRadius
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.RemainingUnit
import com.xixi.notes.ui.util.currentMinute
import com.xixi.notes.ui.util.formatDateTime
import com.xixi.notes.ui.util.remainingTextRes
import androidx.compose.ui.res.stringResource

/** 有截止日期时的行高 */
val TaskRowHeightWithDue = 72.dp

/** 无截止日期时的行高 */
val TaskRowHeightNoDue = 60.dp

/** 任务行卡片圆角 */
private val RowShape = RoundedCornerShape(XixiRadius.medium)

/**
 * 任务行。
 *
 * - 左：Assignee 圆形勾选框 24dp（细线圆环）
 * - 中：标题单行省略；完成时删除线 + 40% 透明
 * - 右侧有图片时显示 Image 图标 12dp
 * - 下方截止日期与剩余时间 12sp，每分钟刷新，过期红色
 * - 右：Checklist 按钮 24dp（细线 × / 绿勾 ✓）
 * - 长按 500ms 触发 [onLongPress]
 *
 * 视觉：卡片底色 + 柔和阴影 + 16dp 圆角；辅助信息用三级文字色；
 * 逾期用危险色；勾选态保持独立的「完成绿」，不并入统一强调色。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(
    task: TaskEntity,
    quadrant: Quadrant,
    highlightAlpha: Float,
    showSeparatedAssigned: Boolean,
    onToggleAssigned: () -> Unit,
    onToggleChecklist: () -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = currentMinute()
    val quadrantColor = XixiTheme.quadrant.colorOf(quadrant)

    val dueText = task.dueDate?.let { due ->
        val remaining = remainingTextRes(due, now)
        val overdue = remaining.unit == RemainingUnit.OVERDUE
        val remainingString = if (overdue) {
            stringResource(com.xixi.notes.R.string.overdue)
        } else {
            stringResource(remaining.stringRes, remaining.value)
        }
        Pair("${formatDateTime(due)} · $remainingString", overdue)
    }

    // 完成时整体 40% 透明
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
                .heightIn(
                    min = if (task.dueDate != null) TaskRowHeightWithDue else TaskRowHeightNoDue
                )
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongPress
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分隔线模式下，已勾选「我来做」的任务再加一层视觉区分
            AssigneeCheckbox(
                checked = task.isAssignedToMe,
                color = quadrantColor,
                onToggle = onToggleAssigned
            )

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(contentAlpha)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        color = XixiTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        letterSpacing = 0.3.sp,
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
                            contentDescription = "含图片",
                            tint = XixiTheme.colors.textTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                if (dueText != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = dueText.first,
                        // 逾期用危险色，其余为辅助信息（更小更淡）
                        color = if (dueText.second) XixiTheme.colors.danger
                        else XixiTheme.colors.textTertiary,
                        fontSize = 12.sp,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            ChecklistButton(
                checked = task.isCheckedOff,
                onToggle = onToggleChecklist
            )
        }
    }
}

/** Assignee 圆形勾选框 24dp */
@Composable
fun AssigneeCheckbox(
    checked: Boolean,
    color: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.86f,
        animationSpec = tween(durationMillis = 220),
        label = "assignee_scale"
    )
    val fill by animateColorAsState(
        targetValue = if (checked) color else Color.Transparent,
        animationSpec = tween(durationMillis = 250),
        label = "assignee_fill"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) color else XixiTheme.colors.textTertiary,
        animationSpec = tween(durationMillis = 250),
        label = "assignee_border"
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
        val tickAlpha by animateFloatAsState(
            targetValue = if (checked) 1f else 0f,
            animationSpec = tween(durationMillis = 180),
            label = "assignee_tick"
        )
        if (tickAlpha > 0.01f) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "切换我的任务",
                // 对勾用专用墨绿，保证在象限色实心圆上的对比度
                tint = XixiTheme.colors.onCheck.copy(alpha = tickAlpha),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** Checklist 按钮 24dp：细线 × / 绿勾 ✓ */
@Composable
fun ChecklistButton(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.06f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "checklist_scale"
    )
    val tickAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "checklist_tick"
    )
    val crossAlpha by animateFloatAsState(
        targetValue = if (checked) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "checklist_cross"
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .scale(scale)
            .clickableNoRipple(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        // 细线 ×
        if (crossAlpha > 0.01f) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "标记完成",
                tint = XixiTheme.colors.textTertiary.copy(alpha = crossAlpha),
                modifier = Modifier.size(18.dp)
            )
        }
        // 绿色勾 ✓（专用「完成绿」，与统一强调色刻意区分）
        if (tickAlpha > 0.01f) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "取消完成",
                tint = XixiTheme.colors.check.copy(alpha = tickAlpha),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** 分组头部卡片圆角（小卡片 16dp） */
private val GroupHeaderShape = RoundedCornerShape(XixiRadius.medium)

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
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        // 数量徽章
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(color.copy(alpha = 0.18f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
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

/** SEPARATED 模式下的分隔线 */
@Composable
fun AssignedDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(XixiTheme.colors.outline)
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
            fontSize = 13.sp,
            letterSpacing = 0.3.sp
        )
    }
}
