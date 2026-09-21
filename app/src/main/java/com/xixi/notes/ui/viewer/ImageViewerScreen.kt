package com.xixi.notes.ui.viewer

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.xixi.notes.R
import com.xixi.notes.di.LocalAppContainer
import com.xixi.notes.ui.components.clickableNoRipple
import com.xixi.notes.ui.theme.Spacing
import com.xixi.notes.ui.theme.ViewerAccent
import com.xixi.notes.ui.theme.ViewerBackground
import com.xixi.notes.ui.theme.ViewerSurface
import com.xixi.notes.ui.theme.ViewerTextPrimary
import com.xixi.notes.ui.theme.ViewerTextSecondary
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.imageModel
import kotlin.math.abs

/** 最小缩放 */
private const val MIN_SCALE = 1.0f

/** 最大缩放 */
private const val MAX_SCALE = 5.0f

/**
 * 全屏图片查看页。
 *
 * - 深色背景 #0A0A0A，隐藏系统栏（离开时恢复 BEHAVIOR_DEFAULT + show）
 * - HorizontalPager，key 用图片相对路径
 * - 每张图独立 scale / offset，进入时重置 1.0
 * - 双指捏合缩放 1.0 ~ 5.0；放大后单指拖动平移；userScrollEnabled = (scale <= 1.0)
 * - 仅在 scale ≈ 1.0 时单击关闭
 * - 单张图片缺失显示 BrokenImage + 「图片已丢失」，仍可左右滑动
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageViewerScreen(
    initialIndex: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val view = LocalView.current

    // 从桥读取本次进入的路径列表（含 "/" 不适合放进路由参数）
    val args = remember { container.imageViewerBridge.consume() }
    val paths = args?.paths.orEmpty()
    val startIndex = (args?.initialIndex ?: initialIndex)
        .coerceIn(0, (paths.size - 1).coerceAtLeast(0))

    // 隐藏系统栏；离开时恢复
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }

    BackHandler { onClose() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViewerBackground)
    ) {
        if (paths.isEmpty()) {
            // 路径列表为空：显示「图片不存在」+ 关闭按钮
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = null,
                    tint = ViewerTextSecondary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = stringResource(R.string.viewer_missing_list),
                    color = ViewerTextPrimary,
                    fontSize = 14.sp,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                Box(
                    modifier = Modifier
                        .clip(XixiTheme.shapes.small)
                        .background(ViewerSurface)
                        .clickableNoRipple(onClick = onClose)
                        .padding(horizontal = Spacing.xl, vertical = Spacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.action_close),
                        color = ViewerTextPrimary,
                        fontSize = 13.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        } else {
            val pagerState = rememberPagerState(
                initialPage = startIndex,
                pageCount = { paths.size }
            )
            // 当前页是否已放大 -> 控制翻页手势
            var currentScale by remember { mutableFloatStateOf(1f) }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { index -> paths[index] },
                userScrollEnabled = currentScale <= 1.01f
            ) { page ->
                ZoomablePage(
                    path = paths[page],
                    onScaleChange = { scale -> currentScale = scale },
                    onSingleTap = onClose
                )
            }

            // 底部页码（只有 1 张时不显示）
            if (paths.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                        .clip(XixiTheme.shapes.small)
                        .background(XixiTheme.colors.sunken.copy(alpha = 0.45f))
                        .padding(horizontal = Spacing.md, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.viewer_page_indicator,
                            pagerState.currentPage + 1,
                            paths.size
                        ),
                        color = ViewerTextPrimary,
                        fontSize = 12.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }

        // 右上角关闭按钮（任何状态可点击，且吞掉整屏点击避免穿透被误判为单击关闭）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { }
                },
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .padding(Spacing.md)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(XixiTheme.colors.sunken.copy(alpha = 0.35f))
                    .clickableNoRipple(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = ViewerTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 单页可缩放图片（每张独立 scale / offset）。
 */
@Composable
private fun ZoomablePage(
    path: String,
    onScaleChange: (Float) -> Unit,
    onSingleTap: () -> Unit
) {
    val container = LocalAppContainer.current
    // 每次进入都重置为 1.0
    val scale = remember(path) { mutableFloatStateOf(1f) }
    val offsetX = remember(path) { mutableFloatStateOf(0f) }
    val offsetY = remember(path) { mutableFloatStateOf(0f) }

    LaunchedEffect(path) {
        scale.floatValue = 1f
        offsetX.floatValue = 0f
        offsetY.floatValue = 0f
        onScaleChange(1f)
    }

    var failed by remember(path) { mutableStateOf(false) }
    if (failed) {
        // 失败状态由 error slot 渲染，这里只保留标记用于无障碍语义
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(path) {
                detectTransformGestures(panZoomLock = true) { _, pan, zoom, _ ->
                    val newScale = (scale.floatValue * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                    scale.floatValue = newScale
                    onScaleChange(newScale)
                    if (newScale > MIN_SCALE + 0.01f) {
                        // 放大后才允许平移
                        offsetX.floatValue += pan.x
                        offsetY.floatValue += pan.y
                    } else {
                        offsetX.floatValue = 0f
                        offsetY.floatValue = 0f
                    }
                }
            }
            .pointerInput(path) {
                detectTapGestures(onTap = {
                    // 仅在 scale ≈ 1.0 时单击关闭
                    if (abs(scale.floatValue - MIN_SCALE) < 0.02f) {
                        onSingleTap()
                    }
                })
            }
            .graphicsLayer {
                scaleX = scale.floatValue
                scaleY = scale.floatValue
                translationX = offsetX.floatValue
                translationY = offsetY.floatValue
            },
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = imageModel(path, container.imageManager),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            loading = {
                // 加载中：居中 CircularProgressIndicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                        color = ViewerAccent
                    )
                }
            },
            error = {
                failed = true
                // 单张缺失：BrokenImage + 「图片已丢失」，仍可左右滑动
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = ViewerTextSecondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        text = stringResource(R.string.viewer_missing_image),
                        color = ViewerTextSecondary,
                        fontSize = 13.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            },
            success = {
                SubcomposeAsyncImageContent()
            }
        )
    }
}
