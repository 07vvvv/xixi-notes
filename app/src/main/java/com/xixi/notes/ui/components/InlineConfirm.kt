package com.xixi.notes.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xixi.notes.ui.util.GentleEasing

/** 深红底色：30% 透明 */
private val DangerBase = Color(0xFF7F1D1D).copy(alpha = 0.30f)

/** 红色扫光 */
private val DangerSheen = Brush.horizontalGradient(
    colors = listOf(
        Color.Transparent,
        Color(0xFFEF4444).copy(alpha = 0.22f),
        Color.Transparent
    )
)

/**
 * Inline Confirm：原地展开的删除确认。
 *
 * 仅重要事项长按触发。点击外部由调用方收起。
 */
@Composable
fun InlineConfirm(
    visible: Boolean,
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200, easing = GentleEasing)) +
            expandVertically(animationSpec = tween(260, easing = GentleEasing)),
        exit = fadeOut(animationSpec = tween(160, easing = GentleEasing)) +
            shrinkVertically(animationSpec = tween(220, easing = GentleEasing)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .animateContentSize(animationSpec = tween(260, easing = GentleEasing))
                .clip(RoundedCornerShape(16.dp))
                .background(DangerBase)
        ) {
            // 红色扫光
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(DangerSheen)
            )

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InlineConfirmButton(
                        label = cancelLabel,
                        emphasized = false,
                        onClick = onCancel
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    InlineConfirmButton(
                        label = confirmLabel,
                        emphasized = true,
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineConfirmButton(
    label: String,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (emphasized) Color(0xFFEF4444).copy(alpha = 0.9f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (emphasized) Color.White else MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
