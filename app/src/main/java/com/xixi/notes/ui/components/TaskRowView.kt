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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xixi.notes.data.local.TaskEntity
import com.xixi.notes.ui.board.Quadrant
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

/**
 * 任务行。
 *
 * - 左：Assignee 圆形勾选框 24dp（细线圆环）
 * - 中：标题单行省略；完成时删除线 + 40% 透明
 * - 右侧有图片时显示 Image 图标 12dp
 * - 下方截止日期与剩余时间 12sp，每分钟刷新，过期红色
 * - 右：Checklist 按钮 24dp（细线 × / 绿勾 ✓）
 * - 长按 500ms 触发 [onLongPress]
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

    // 高亮闪烁：#6EE7B7 的 20% 透明 -> 0%
    val highlightColor = Color(0xFF6EE7B7).copy(alpha = 0.20f * highlightAlpha)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分隔线模式下，已勾选「我来做」的任务再加一层视觉区分
            AssigneeCheckbox(
                checked = task.isAssignedToMe,
                color = quadrantColor,
                onToggle = onToggleAssigned
            )

            Spacer(modifier = Modifier.width(12.dp))

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
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isCheckedOff) TextDecoration.LineThrough
                        else TextDecoration.None,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (task.imagePaths.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "含图片",
                            tint = XixiTheme.colors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                if (dueText != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = dueText.first,
                        color = if (dueText.second) Color(0xFFEF4444)
                        else XixiTheme.colors.textSecondary,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

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
        targetValue = if (checked) color else XixiTheme.colors.textSecondary,
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
                tint = Color(0xFF0A0A0A).copy(alpha = tickAlpha),
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
                tint = XixiTheme.colors.textSecondary.copy(alpha = crossAlpha),
                modifier = Modifier.size(18.dp)
            )
        }
        // 绿色勾 ✓
        if (tickAlpha > 0.01f) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "取消完成",
                tint = Color(0xFF22C55E).copy(alpha = tickAlpha),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** 分组头部 56dp：分组名 + 数量徽章 + 折叠箭头 */
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
            .height(56.dp)
            .clickableNoRipple(onClick = onToggleFold)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 象限色圆点
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = XixiTheme.colors.textPrimary,
            fontSize = 14.sp,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        // 数量徽章
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.18f))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                color = color,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        // 折叠箭头：折叠时旋转 -90°
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (folded) "展开分组" else "折叠分组",
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
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(XixiTheme.colors.textSecondary.copy(alpha = 0.25f))
        )
    }
}
