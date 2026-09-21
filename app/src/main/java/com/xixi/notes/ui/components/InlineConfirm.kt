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
import androidx.compose.foundation.layout.width
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
import com.xixi.notes.ui.theme.Spacing
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.GentleEasing

/**
 * Inline Confirm：原地展开的删除确认。
 *
 * 仅重要事项长按触发。点击外部由调用方收起。
 *
 * 视觉：危险语义底色（危险色 18%）+ 一条缓慢横移的危险色扫光，
 * 文字使用主题主/次文字色，确认按钮为危险色实心。
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
                .padding(top = Spacing.xs)
                .animateContentSize(animationSpec = tween(260, easing = GentleEasing))
                .clip(XixiTheme.shapes.item)
                .background(XixiTheme.colors.dangerContainer)
        ) {
            // 危险色扫光
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                XixiTheme.colors.danger.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(
                    text = title,
                    color = XixiTheme.colors.onDangerContainer,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = message,
                    color = XixiTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(Spacing.md))
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
                    Spacer(modifier = Modifier.width(Spacing.sm))
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
            .height(34.dp)
            .clip(XixiTheme.shapes.thumbnail)
            .background(
                if (emphasized) XixiTheme.colors.danger
                else XixiTheme.colors.sunken
            )
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = Spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            // 危险色实心上用危险容器的前景色保证对比度
            color = if (emphasized) XixiTheme.colors.onDangerContainer
            else XixiTheme.colors.textPrimary,
            fontSize = 13.sp,
            letterSpacing = 0.3.sp,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
