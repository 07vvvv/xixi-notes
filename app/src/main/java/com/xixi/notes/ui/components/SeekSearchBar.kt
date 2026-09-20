package com.xixi.notes.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xixi.notes.ui.util.GentleEasing

/** 闭合直径 */
private val COLLAPSED = 44.dp

/**
 * Seek 搜索框（无状态）。
 *
 * - 闭合：44dp 圆形，图标左 inset 13dp
 * - 展开：胶囊，宽度 min(320dp, 屏幕宽度 - 32dp)
 * - 右侧 X 关闭并清空
 * - p = (w - 44) / (OPEN - 44)，占位符透明度取后三分之一
 * - 按压先压缩 90ms，焦点随扩展到达
 * - 磁铁效果：位移最大 7dp，打开后归零
 */
@Composable
fun SeekSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
    expanded: Boolean,
    onExpandRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val openWidth: Dp = minOf(320.dp, screenWidth - 32.dp)
    val expandedWidth = if (openWidth > COLLAPSED) openWidth else COLLAPSED

    // 按压先压缩 90ms
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "seek_press"
    )

    // 展开进度
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = GentleEasing),
        label = "seek_expand"
    )

    val currentWidth: Dp = COLLAPSED + (expandedWidth - COLLAPSED) * progress
    // 占位符只在展开的最后三分之一显示
    val placeholderAlpha: Float = ((progress - 2f / 3f) / (1f / 3f)).coerceIn(0f, 1f)
    val closeAlpha: Float = ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f)
    // 磁铁效果：展开后关闭位移
    val magnetOffset: Dp = 7.dp * (1f - progress)

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // 焦点随扩展到达
    LaunchedEffect(expanded, progress) {
        if (expanded && progress > 0.98f) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Box(
        modifier = modifier
            .offset(x = -magnetOffset)
            .height(COLLAPSED * pressScale)
            .width(currentWidth * pressScale),
        contentAlignment = Alignment.CenterStart
    ) {
        // 背景：闭合为圆形，展开过渡为胶囊
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(if (progress > 0.55f) RoundedCornerShape(percent = 50) else CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickableNoRipple(
                    interactionSource = interactionSource,
                    onClick = { if (!expanded) onExpandRequest() }
                )
        )

        if (expanded) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, end = 38.dp)
                    .focusRequester(focusRequester)
            )
        }

        if (placeholderAlpha > 0.01f) {
            Text(
                text = "搜索任务…",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = placeholderAlpha),
                fontSize = 14.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 40.dp)
            )
        }

        // 左侧放大镜：左 inset 13dp
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "搜索",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 13.dp)
                .size(18.dp)
        )

        // 右侧 X：关闭并清空
        if (closeAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickableNoRipple {
                        onValueChange("")
                        onClose()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭搜索",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = closeAlpha),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
