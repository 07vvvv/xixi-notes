package com.xixi.notes.ui.main

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xixi.notes.di.LocalAppContainer
import com.xixi.notes.di.RadialMenuState
import com.xixi.notes.ui.archive.ArchiveScreen
import com.xixi.notes.ui.board.BoardScreen
import com.xixi.notes.ui.board.Quadrant
import com.xixi.notes.ui.components.DockHeight
import com.xixi.notes.ui.components.DockTab
import com.xixi.notes.ui.components.DockView
import com.xixi.notes.ui.components.RadialMenuOverlay
import com.xixi.notes.ui.components.clickableNoRipple
import com.xixi.notes.ui.detail.DetailScreen
import com.xixi.notes.ui.onboarding.OnboardingScreen
import com.xixi.notes.ui.settings.SettingsScreen
import com.xixi.notes.ui.stats.StatsScreen
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.GentleEasing
import com.xixi.notes.ui.viewer.ImageViewerScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 路由常量 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val BOARD = "board"
    const val EDIT = "edit"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val ARCHIVE = "archive"
    const val IMAGE_VIEWER = "image_viewer"

    /** edit/{taskId}?quadrant={quadrantName}；taskId = -1 表示新建 */
    const val EDIT_PATTERN = "$EDIT/{taskId}?quadrant={quadrant}"
    const val IMAGE_VIEWER_PATTERN = "$IMAGE_VIEWER/{initialIndex}"

    fun edit(taskId: Long, quadrant: String? = null): String =
        if (quadrant == null) "$EDIT/$taskId" else "$EDIT/$taskId?quadrant=$quadrant"

    fun imageViewer(initialIndex: Int) = "$IMAGE_VIEWER/$initialIndex"
}

/** Dock 标签 -> 路由 */
private fun DockTab.route(): String = when (this) {
    DockTab.BOARD -> Routes.BOARD
    DockTab.STATS -> Routes.STATS
    DockTab.ARCHIVE -> Routes.ARCHIVE
    DockTab.SETTINGS -> Routes.SETTINGS
}

private fun String?.toDockTab(): DockTab = when {
    this == null -> DockTab.BOARD
    startsWith(Routes.STATS) -> DockTab.STATS
    startsWith(Routes.ARCHIVE) -> DockTab.ARCHIVE
    startsWith(Routes.SETTINGS) -> DockTab.SETTINGS
    else -> DockTab.BOARD
}

/**
 * 根 Scaffold。
 *
 * 层级（自下而上）：
 * NavHost -> RadialMenuOverlay -> Dock -> FAB
 */
@Composable
fun MainScaffold() {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // 主屏列表状态提升到根，便于 Dock / FAB 感知滚动
    val listScrollState: LazyListState = rememberLazyListState()

    val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModel.Factory(
            container.taskRepository,
            container.preferences,
            container.reminderScheduler
        )
    )

    val appState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val undo by mainViewModel.undo.collectAsStateWithLifecycle()
    val toast by mainViewModel.toast.collectAsStateWithLifecycle()
    val scrollToTopTick by mainViewModel.scrollToTopTick.collectAsStateWithLifecycle()
    val radialMenuState by container.radialMenuState.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // 顶部栏状态（由 Board 上报，驱动 FAB 显隐）
    var searchExpanded by remember { mutableStateOf(false) }
    var fabVisibleByScroll by remember { mutableStateOf(true) }

    // 滚动时 Dock 整体缩小至 0.85
    val isScrolling by remember { derivedStateOf { listScrollState.isScrollInProgress } }
    val scrollingScale by animateFloatAsState(
        targetValue = if (isScrolling) 0.85f else 1f,
        animationSpec = tween(durationMillis = 220, easing = GentleEasing),
        label = "dock_scroll_scale"
    )

    // FAB 中心坐标（Radial Menu 以它为圆心）
    var fabCenterX by remember { mutableStateOf(0f) }
    var fabCenterY by remember { mutableStateOf(0f) }

    val isBoard = currentRoute == Routes.BOARD
    val isOnboarding = currentRoute == Routes.ONBOARDING
    val isEditor = currentRoute?.startsWith(Routes.EDIT) == true
    val isViewer = currentRoute?.startsWith(Routes.IMAGE_VIEWER) == true
    val showChrome = !isOnboarding && !isEditor && !isViewer

    // 通知导航事件
    LaunchedEffect(Unit) {
        container.navEvents.collect { event ->
            mainViewModel.consumeNavEvent(event) { taskId ->
                navController.navigate(Routes.edit(taskId))
            }
        }
    }

    // Dock 缩放：滚动时 0.85，离开主屏时也收缩
    val animatedDockScale by animateFloatAsState(
        targetValue = if (showChrome) scrollingScale else 0.85f,
        animationSpec = tween(durationMillis = 260, easing = GentleEasing),
        label = "dock_scale"
    )

    // FAB 位置随 Dock 缩放同步动画
    val fabBottomPadding by animateDpAsState(
        targetValue = DockHeight * animatedDockScale + 16.dp,
        animationSpec = tween(durationMillis = 260, easing = GentleEasing),
        label = "fab_bottom"
    )

    // 窗口 insets：底部系统栏 ∪ IME
    val windowInsets = WindowInsets.navigationBars
        .only(WindowInsetsSides.Bottom)
        .union(WindowInsets.ime)

    BackHandler(enabled = isBoard) {
        // 主屏根页面：退到后台，不销毁任务状态
        (context as? Activity)?.moveTaskToBack(true)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ---------------------------------------------------------- NavHost
        NavHost(
            navController = navController,
            startDestination = Routes.ONBOARDING,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Routes.BOARD) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.BOARD) {
                BoardScreen(
                    mainViewModel = mainViewModel,
                    appState = appState,
                    undo = undo,
                    toast = toast,
                    scrollToTopTick = scrollToTopTick,
                    onOpenEditor = { taskId -> navController.navigate(Routes.edit(taskId)) },
                    onOpenImageViewer = { index ->
                        navController.navigate(Routes.imageViewer(index))
                    },
                    onSearchExpandedChange = { searchExpanded = it },
                    onScrollVisibilityChange = { visible -> fabVisibleByScroll = visible },
                    listState = listScrollState
                )
            }

            composable(
                route = Routes.EDIT_PATTERN,
                arguments = listOf(
                    navArgument("taskId") { type = NavType.LongType },
                    navArgument("quadrant") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val taskId = entry.arguments?.getLong("taskId") ?: -1L
                val quadrantName = entry.arguments?.getString("quadrant")
                val initialQuadrant = quadrantName?.let { name ->
                    runCatching { Quadrant.valueOf(name) }.getOrNull()
                }
                DetailScreen(
                    taskId = taskId,
                    initialQuadrant = initialQuadrant,
                    onClose = { navController.popBackStack() },
                    onOpenImageViewer = { index ->
                        navController.navigate(Routes.imageViewer(index))
                    }
                )
            }

            composable(Routes.STATS) {
                StatsScreen()
            }

            composable(Routes.ARCHIVE) {
                ArchiveScreen(
                    highlightedTaskId = appState.highlightTaskId,
                    highlightNonce = appState.highlightNonce,
                    onOpenEditor = { taskId -> navController.navigate(Routes.edit(taskId)) },
                    onOpenImageViewer = { index ->
                        navController.navigate(Routes.imageViewer(index))
                    }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenOnboarding = { navController.navigate(Routes.ONBOARDING) }
                )
            }

            composable(
                route = Routes.IMAGE_VIEWER_PATTERN,
                arguments = listOf(navArgument("initialIndex") { type = NavType.IntType })
            ) { entry ->
                val initialIndex = entry.arguments?.getInt("initialIndex") ?: 0
                ImageViewerScreen(
                    initialIndex = initialIndex,
                    onClose = { navController.popBackStack() }
                )
            }
        }

        // ------------------------------------------------- Radial Menu（遮罩 + 菜单）
        val menuState = radialMenuState
        if (menuState != null) {
            RadialMenuOverlay(
                state = menuState,
                onSelect = { quadrant ->
                    // 先关闭菜单，再进编辑页（新建任务预置象限）
                    container.radialMenuState.value = null
                    navController.navigate(Routes.edit(-1L, quadrant.name))
                },
                onDismiss = { container.radialMenuState.value = null },
                modifier = Modifier.zIndex(2f)
            )
        }

        // ------------------------------------------------------------------ Dock
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(animationSpec = tween(220, easing = GentleEasing)),
            exit = fadeOut(animationSpec = tween(160, easing = GentleEasing)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(0.5f)
                .then(
                    // 退出过程中吞掉点击，避免误触底层列表
                    if (showChrome) Modifier
                    else Modifier.pointerInput(Unit) {
                        awaitPointerEventScope { while (true) awaitPointerEvent() }
                    }
                )
        ) {
            DockView(
                selected = currentRoute.toDockTab(),
                scale = animatedDockScale,
                onSelect = { tab ->
                    val route = tab.route()
                    if (currentRoute.toDockTab() == tab) {
                        // 点击当前 tab：滚动到顶部
                        mainViewModel.requestScrollToTop()
                    } else {
                        // 切换 tab 时立即确认撤销
                        mainViewModel.clearUndo()
                        navController.navigate(route) {
                            popUpTo(Routes.BOARD) {
                                inclusive = route == Routes.BOARD
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier
                    .background(XixiTheme.colors.background)
                    .windowInsetsPadding(windowInsets)
            )
        }

        // ------------------------------------------------------------------- FAB
        val fabShouldShow = showChrome && isBoard && !searchExpanded && fabVisibleByScroll
        AnimatedVisibility(
            visible = fabShouldShow,
            enter = fadeIn(animationSpec = tween(200, easing = GentleEasing)) +
                scaleIn(initialScale = 0.8f, animationSpec = tween(220, easing = GentleEasing)),
            exit = fadeOut(animationSpec = tween(150, easing = GentleEasing)) +
                scaleOut(targetScale = 0.8f, animationSpec = tween(160, easing = GentleEasing)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .zIndex(1f)
                .padding(end = 20.dp, bottom = fabBottomPadding + 8.dp)
                .windowInsetsPadding(windowInsets)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInWindow()
                        fabCenterX = bounds.center.x
                        fabCenterY = bounds.center.y
                    }
                    .clickableNoRipple {
                        // 打开 Radial Menu：以 FAB 中心为圆心
                        container.radialMenuState.value = RadialMenuState(fabCenterX, fabCenterY)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建任务",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    // 撤销窗口结束后真正删除图片文件（应用被杀时任务被删、图片保留）
    LaunchedEffect(undo?.token) {
        val slot = undo ?: return@LaunchedEffect
        delay(3_000L)
        if (mainViewModel.undo.value?.token == slot.token) {
            mainViewModel.clearUndo()
            slot.cancelImageDeletionTaskId?.let { taskId ->
                scope.launch { container.taskRepository.deleteTaskImages(taskId) }
            }
        }
    }
}
