package com.xixi.notes.ui.archive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xixi.notes.R
import com.xixi.notes.data.local.TaskEntity
import com.xixi.notes.ui.board.Quadrant
import com.xixi.notes.ui.components.clickableNoRipple
import com.xixi.notes.ui.components.colorOf
import com.xixi.notes.ui.theme.Spacing
import com.xixi.notes.ui.theme.XixiElevation
import com.xixi.notes.ui.theme.XixiItemShape
import com.xixi.notes.ui.theme.XixiTextStyles
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.formatDateTime

/** 归档行卡片圆角（与主屏任务行一致，条目卡片 16dp） */
private val ArchiveRowShape = XixiItemShape

/**
 * 归档任务行。
 *
 * 视觉与主屏任务行保持同一套卡片规范（16dp 圆角 + 2dp 柔和阴影 + 卡片底色 + 留白），
 * 保证「主屏 → 归档」的观感连续；支持取消完成与永久删除，并显示完成时间。
 */
@Composable
fun ArchiveRow(
    task: TaskEntity,
    highlightAlpha: Float,
    onUncomplete: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quadrant = Quadrant.of(task)
    val quadrantColor = XixiTheme.quadrant.colorOf(quadrant)
    val completedAt = task.completedAt ?: task.updatedAt

    // 高亮闪烁：统一强调色的 20% 透明 -> 0%
    val highlightColor by animateColorAsState(
        targetValue = XixiTheme.colors.accent.copy(alpha = 0.20f * highlightAlpha),
        animationSpec = tween(durationMillis = 200),
        label = "archive_highlight"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = 3.dp)
            .shadow(XixiElevation.card, ArchiveRowShape)
            .clip(ArchiveRowShape)
            .background(XixiTheme.colors.card)
            .background(highlightColor)
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 象限色圆点
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(quadrantColor)
        )
        Spacer(modifier = Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = task.title,
                    // 已完成：整体降透明度 + 删除线
                    color = XixiTheme.colors.textPrimary.copy(alpha = 0.75f),
                    style = XixiTextStyles.rowTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = TextDecoration.LineThrough,
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
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = stringResource(
                    R.string.archive_completed_at,
                    formatDateTime(completedAt)
                ),
                color = XixiTheme.colors.textTertiary,
                style = XixiTextStyles.minor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        // 取消完成：使用「完成」专用绿
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickableNoRipple(onClick = onUncomplete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.archive_uncomplete),
                tint = XixiTheme.colors.check,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.xs))

        // 永久删除：危险色
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickableNoRipple(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.action_delete),
                tint = XixiTheme.colors.danger,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** 归档搜索结果的空提示 */
@Composable
fun ArchiveNoResult(onClear: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.search_no_result_archive),
            color = XixiTheme.colors.textPrimary,
            style = XixiTextStyles.rowTitle,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Box(
            modifier = Modifier
                .shadow(XixiElevation.card, XixiTheme.shapes.small)
                .clip(XixiTheme.shapes.small)
                .background(XixiTheme.colors.card)
                .clickableNoRipple(onClick = onClear)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.action_clear_search),
                color = XixiTheme.colors.textPrimary,
                style = XixiTextStyles.caption,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
