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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xixi.notes.R
import com.xixi.notes.data.local.TaskEntity
import com.xixi.notes.ui.board.Quadrant
import com.xixi.notes.ui.components.clickableNoRipple
import com.xixi.notes.ui.components.colorOf
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.formatDateTime

/**
 * 归档任务行。
 *
 * 支持取消完成与永久删除，并显示完成时间。
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

    val highlightColor by animateColorAsState(
        targetValue = Color(0xFF6EE7B7).copy(alpha = 0.20f * highlightAlpha),
        animationSpec = tween(durationMillis = 200),
        label = "archive_highlight"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(XixiTheme.colors.card)
            .background(highlightColor)
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 象限色圆点
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(quadrantColor)
        )
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = task.title,
                    color = XixiTheme.colors.textPrimary.copy(alpha = 0.75f),
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (task.imagePaths.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = stringResource(R.string.cd_has_image),
                        tint = XixiTheme.colors.textSecondary,
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
                color = XixiTheme.colors.textSecondary,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 取消完成
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickableNoRipple(onClick = onUncomplete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.archive_uncomplete),
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 永久删除
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickableNoRipple(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.action_delete),
                tint = Color(0xFFEF4444),
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.search_no_result_archive),
            color = XixiTheme.colors.textPrimary,
            fontSize = 15.sp,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(XixiTheme.colors.card)
                .clickableNoRipple(onClick = onClear)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.action_clear_search),
                color = XixiTheme.colors.textPrimary,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
