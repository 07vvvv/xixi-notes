package com.xixi.notes.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.xixi.notes.R
import com.xixi.notes.ui.components.clickableNoRipple
import com.xixi.notes.ui.theme.Spacing
import com.xixi.notes.ui.theme.XixiElevation
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.GentleEasing
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * 引导页每一页的视觉配置。
 *
 * [motion] 决定该页图标的 Compose 自绘动效类型（项目内无 Lottie 资源文件，
 * 按约定用 Compose 模拟，不伪造 Lottie JSON）：
 * - PULSE：光环呼吸（重要与紧急）
 * - ORBIT：手指左右往复滑动（Radial Menu）
 * - RING：铃铛上下轻摆（提醒与主题）
 */
private enum class PageMotion { PULSE, ORBIT, RING }

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
    val motion: PageMotion
)

private val pages = listOf(
    OnboardingPage(
        Icons.Default.CheckCircle,
        R.string.onboarding_page1_title,
        R.string.onboarding_page1_body,
        PageMotion.PULSE
    ),
    OnboardingPage(
        Icons.Default.TouchApp,
        R.string.onboarding_page2_title,
        R.string.onboarding_page2_body,
        PageMotion.ORBIT
    ),
    OnboardingPage(
        Icons.Default.Notifications,
        R.string.onboarding_page3_title,
        R.string.onboarding_page3_body,
        PageMotion.RING
    )
)

/**
 * 引导页：首次启动无条件展示 3 页。
 *
 * - HorizontalPager + 底部指示器
 * - 每页配动画（Compose 自绘：脉冲光环 / 往复滑动 / 轻摆）
 * - 底部「下一步 / 开始使用」，右上角「跳过」
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XixiTheme.colors.background)
            .systemBarsPadding()
    ) {
        // 右上角跳过
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Spacing.lg)
                .clip(XixiTheme.shapes.small)
                .clickableNoRipple { onFinish() }
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.onboarding_skip),
                color = XixiTheme.colors.textSecondary,
                fontSize = 13.sp,
                letterSpacing = 0.3.sp
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val pageOffset = (
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    ).absoluteValue
                val contentAlpha by animateFloatAsState(
                    targetValue = lerp(1f, 0.3f, pageOffset.coerceIn(0f, 1f)),
                    animationSpec = tween(durationMillis = 200, easing = GentleEasing),
                    label = "onboarding_page_alpha"
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = contentAlpha }
                        .padding(horizontal = Spacing.huge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PageAnimation(motion = pages[page].motion, icon = pages[page].icon)
                    Spacer(modifier = Modifier.height(Spacing.huge))
                    Text(
                        text = stringResource(pages[page].titleRes),
                        color = XixiTheme.colors.textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        text = stringResource(pages[page].bodyRes),
                        color = XixiTheme.colors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 23.sp,
                        letterSpacing = 0.3.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 指示器：选中项拉长
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = Spacing.xxl)
            ) {
                pages.indices.forEach { index ->
                    val selected = pagerState.currentPage == index
                    val indicatorWidth by animateFloatAsState(
                        targetValue = if (selected) 22f else 8f,
                        animationSpec = tween(durationMillis = 240, easing = GentleEasing),
                        label = "onboarding_indicator"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = Spacing.xs)
                            .size(width = indicatorWidth.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) XixiTheme.colors.accent
                                else XixiTheme.colors.outline
                            )
                    )
                }
            }

            // 下一步 / 开始使用（与 pager 同步）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.huge)
                    .padding(bottom = Spacing.huge)
                    .height(52.dp)
                    .shadow(XixiElevation.cardStrong, XixiTheme.shapes.item)
                    .clip(XixiTheme.shapes.item)
                    .background(XixiTheme.colors.accent)
                    .clickableNoRipple {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(
                        if (isLastPage) R.string.onboarding_start else R.string.onboarding_next
                    ),
                    color = XixiTheme.colors.onAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

/**
 * 每页的图标动画（Compose 自绘，统一围绕强调色）。
 *
 * - PULSE：图标放大缩小的同时，底部光环同步呼吸扩散
 * - ORBIT：图标整体左右往复滑动（呼应 Radial Menu 的拖动手势）
 * - RING：图标轻微左右摆动（呼应提醒 / 主题切换的"活起来"）
 */
@Composable
private fun PageAnimation(
    motion: PageMotion,
    icon: ImageVector
) {
    val infinite = rememberInfiniteTransition(label = "onboarding_motion")
    // 统一的呼吸量，供三种动效复用
    val breath by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = GentleEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboarding_breath"
    )

    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // 光环：PULSE 页最明显，其余两页保持淡雅
        val haloAlpha = when (motion) {
            PageMotion.PULSE -> 0.10f + 0.16f * breath
            else -> 0.08f + 0.06f * breath
        }
        val haloScale = when (motion) {
            PageMotion.PULSE -> 0.86f + 0.24f * breath
            else -> 0.94f + 0.08f * breath
        }
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(XixiTheme.colors.accent.copy(alpha = haloAlpha))
                .graphicsLayer {
                    scaleX = haloScale
                    scaleY = haloScale
                }
        )

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = XixiTheme.colors.accent,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    when (motion) {
                        PageMotion.PULSE -> {
                            val s = 1f + 0.06f * breath
                            scaleX = s
                            scaleY = s
                        }
                        PageMotion.ORBIT -> {
                            translationX = (-14f + 28f * breath)
                        }
                        PageMotion.RING -> {
                            rotationZ = -7f + 14f * breath
                        }
                    }
                }
        )
    }
}
