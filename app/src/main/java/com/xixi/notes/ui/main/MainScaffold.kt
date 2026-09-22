package com.xixi.notes.ui.main

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.xixi.notes.ui.components.FabSize
import com.xixi.notes.ui.components.RadialMenuHost
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

    /** 主屏可带筛选参数：board?filter=IMPORTANT_URGENT / OVERDUE */
    const val BOARD_PATTERN = "$BOARD?filter={filter}"

    /** edit/{taskId}?quadrant={quadrantName}；taskId = -1 表示新建 */
    const val EDIT_PATTERN = "$EDIT/{taskId}?quadrant={quadrant}"
    const val IMAGE_VIEWER_PATTERN = "$IMAGE_VIEWER/{initialIndex}"

    fun board(filter: String? = null): String =
        if (filter == null) BOARD else "$BOARD?filter=$filter"

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

/**
 * 页面切换过渡时长（ms）。
 *
 * 只做极简 alpha 过渡：进入 0.95 -> 1.0、退出 1.0 -> 0.95，两个方向同长。
 * 过渡期间露出的底色由根 Box 的主题背景 + 窗口背景兜底，绝不会是黑色。
 * 若仍能肉眼看到黑幕，把 [NavScreenTransitionDurationMs] 设为 0 即可完全关闭过渡。
 */
private const val NavScreenTransitionDurationMs = 150

/** 进入过渡：alpha 0.95 -> 1.0（无位移、无缩放、无黑色） */
private val navEnterTransition: EnterTransition =
    fadeIn(animationSpec = tween(durationMillis = NavScreenTransitionDurationMs, easing = GentleEasing))

/** 退出过渡：alpha 1.0 -> 0.95（与进入同长，避免两页叠加出现暗带） */
private val navExitTransition: ExitTransition =
    fadeOut(animationSpec = tween(durationMillis = NavScreenTransitionDurationMs, easing = GentleEasing))

/**
 * 取路由的路径部分（丢掉查询参数）。
 *
 * `NavDestination.route` 返回的是**注册时的模式串**，例如主屏注册为
 * `board?filter={filter}`，直接与 `Routes.BOARD` 比较会永远不相等，
 * 导致 isBoard / Dock 高亮等判断全部失效。所有比较都必须先归一化。
 */
private fun String?.routePath(): String? = this?.substringBefore('?')

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
 * NavHost -> RadialMenu（遮罩 + 象限按钮）-> Dock -> FAB
 *
 * @param onboardingShown 引导页是否已展示过（false 时起始路由为 onboarding）
 */
@Composable
fun MainScaffold(
    onboardingShown: Boolean
) {
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

    // FAB 中心坐标（Radial Menu 以它为圆心）。
    // 用屏幕尺寸初始化，避免首帧测得 (0,0) 导致菜单闪现在左上角
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val defaultFabX = with(density) { configuration.screenWidthDp.dp.toPx() } / 2f
    val defaultFabY = with(density) { configuration.screenHeightDp.dp.toPx() }
    var fabCenterX by remember(defaultFabX) { mutableStateOf(defaultFabX) }
    var fabCenterY by remember(defaultFabY) { mutableStateOf(defaultFabY) }

    // 注意：主屏注册的是 "board?filter={filter}"，currentRoute 会带上查询参数，
    // 因此这里必须用 routePath() 归一化，否则 isBoard 恒为 false（FAB 会整个不渲染）
    val currentPath = currentRoute.routePath()
    val isBoard = currentPath == Routes.BOARD
    val isOnboarding = currentPath == Routes.ONBOARDING
    val isEditor = currentPath?.startsWith(Routes.EDIT) == true
    val isViewer = currentPath?.startsWith(Routes.IMAGE_VIEWER) == true
    val showChrome = !isOnboarding && !isEditor && !isViewer

    /**
     * 进入主屏（唯一入口）。
     *
     * - 主屏只有一种状态：路由参数 `board?filter={filter}`，filter 为 null 即完整列表。
     *   所以「展示筛选」和「清除筛选」是同一件事：按新的 filter 重建一次主屏。
     * - 需要调 filter 时先 `popUpTo(BOARD_PATTERN) { inclusive = true }` 把旧主屏 entry
     *   弹掉，避免出现两个 board 实例（那正是返回键失效、Dock 主页点不动的根源）。
     * - 若主屏就是栈顶且无需换 filter（例如 Dock 主页点自己），
     *   则只 `launchSingleTop` 原地更新参数：既清掉筛选又不会把整个返回栈弹空。
     */
    fun navigateBoard(filter: String?) {
        navController.navigate(Routes.board(filter)) {
            // 只有在"当前不在主屏"时才需要把栈里的旧主屏 entry 弹掉：
            // 此时旧 entry 一定在主屏下方，弹掉它不会把返回栈弹空，能避免出现两个 board 实例。
            // 若已经站在主屏上（例如 Dock 主页点自己），只靠 launchSingleTop 原地更新参数即可，
            // 既换了 filter 又不会破坏返回栈。
            if (!isBoard) {
                popUpTo(Routes.BOARD_PATTERN) { inclusive = true }
            }
            launchSingleTop = true
        }
    }

    /**
     * 清掉筛选。
     *
     * @param returnToStats true：返回键触发 —— 清筛选，并且如果本次是从统计页筛选进来的，
     *   顺带退回统计页；false：筛选条 ✕ 触发 —— 只清筛选，留在主屏看完整列表。
     *
     * - 需要回统计页时，先 `popBackStack()` 弹掉带 filter 的旧主屏 entry，再建立目标页面：
     *   这一步必须在 [navigateBoard] **之前**完成——它是相对"当前栈顶"作栈内回退；
     *   若放到之后，栈顶已经变成新建的主屏 entry，再回退就变成"退出主屏"了。
     * - 只有「主屏正下方的可见页面是统计页」时才回统计页：
     *   直接开在主屏（没有从统计页进来）时，旧主屏 entry 就是栈底，弹掉它只会让应用退出。
     */
    fun clearFilter(returnToStats: Boolean) {
        val fromStats = backStackEntry?.previousBackStackEntry
            ?.destination?.route?.routePath() == Routes.STATS
        if (returnToStats && fromStats && navController.popBackStack()) {
            navController.navigate(Routes.STATS) {
                popUpTo(Routes.BOARD_PATTERN) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            navigateBoard(null)
        }
    }

    // 通知导航事件
    LaunchedEffect(Unit) {
        container.navEvents.collect { event ->
            mainViewModel.consumeNavEvent(event) { taskId ->
                navController.navigate(Routes.edit(taskId))
            }
        }
    }

    // 首次启动权限申请：先通知，后精确闹钟
    val startupPermission = rememberStartupPermissions(context)
    LaunchedEffect(onboardingShown, isOnboarding) {
        if (onboardingShown && !isOnboarding) {
            val alreadyRequested = container.preferences.isPermissionsRequested()
            if (!alreadyRequested) {
                startupPermission.requestAll()
                container.preferences.setPermissionsRequested(true)
            }
        }
    }

    // Dock 缩放：滚动时 0.85，离开主屏时也收缩
    val animatedDockScale by animateFloatAsState(
        targetValue = if (showChrome) scrollingScale else 0.85f,
        animationSpec = tween(durationMillis = 260, easing = GentleEasing),
        label = "dock_scale"
    )

    // FAB 到 Dock 上方的距离：24dp + Dock 高度（跟随 Dock 缩放同步动画）
    val fabBottomPadding by animateDpAsState(
        targetValue = DockHeight * animatedDockScale + 24.dp,
        animationSpec = tween(durationMillis = 260, easing = GentleEasing),
        label = "fab_bottom"
    )

    // 窗口 insets：底部系统栏 ∪ IME（FAB 占位层仍然需要它来避让系统栏；
    // Dock 自己内部也按同一套 insets 避让，两处互不影响）
    val windowInsets = WindowInsets.navigationBars
        .only(WindowInsetsSides.Bottom)
        .union(WindowInsets.ime)

    BackHandler(enabled = isBoard) {
        // 主屏根页面：退到后台，不销毁任务状态
        (context as? Activity)?.moveTaskToBack(true)
    }

    // 根容器必须自带主题背景色：
    // 否则页面切换过渡期间（页面 alpha < 1）会直接露出窗口背景，
    // 配合"深色 windowBackground"就表现为「黑色半透明遮罩一闪而过」。
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(XixiTheme.colors.background)
    ) {

        // 页面切换过渡：显式声明、只做极简 alpha，杜绝任何黑色遮罩。
        // 进入 0.95 -> 1.0、退出 1.0 -> 0.95（同长 150ms、同曲线），两页叠加期
        // 最低合成不透明度约 0.9975，肉眼不可见；即使透底，露出的也是同色主题背景。
        // 若设备上仍能看到黑幕：把 NavScreenTransitionDurationMs 改成 0 即完全关闭过渡。
        NavHost(
            navController = navController,
            // 引导页只展示一次：读 DataStore 决定起始路由
            startDestination = if (onboardingShown) Routes.BOARD else Routes.ONBOARDING,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { navEnterTransition },
            exitTransition = { navExitTransition },
            popEnterTransition = { navEnterTransition },
            popExitTransition = { navExitTransition }
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinish = {
                        // 先写入 DataStore，再导航，避免下次启动重复弹出
                        scope.launch {
                            container.preferences.setOnboardingShown(true)
                            navController.navigate(Routes.BOARD) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(
                route = Routes.BOARD_PATTERN,
                arguments = listOf(
                    navArgument("filter") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val filterArg = entry.arguments?.getString("filter")
                BoardScreen(
                    mainViewModel = mainViewModel,
                    appState = appState,
                    undo = undo,
                    toast = toast,
                    scrollToTopTick = scrollToTopTick,
                    listState = listScrollState,
                    initialFilter = filterArg,
                    onOpenEditor = { taskId -> navController.navigate(Routes.edit(taskId)) },
                    onOpenImageViewer = { index ->
                        navController.navigate(Routes.imageViewer(index))
                    },
                    onSearchExpandedChange = { searchExpanded = it },
                    onScrollVisibilityChange = { visible -> fabVisibleByScroll = visible },
                    // 返回键：清筛选（本次从统计页进来的话顺带回统计页）；
                    // 筛选条 ✕：只清筛选，留在主屏
                    onClearFilter = { returnToStats -> clearFilter(returnToStats) },
                    onExitApp = { (context as? Activity)?.moveTaskToBack(true) }
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
                StatsScreen(
                    // 主屏只有路由参数一个筛选来源：点卡片即带上对应 filter 重建主屏
                    onQuadrantClick = { quadrant -> navigateBoard(quadrant.name) },
                    onCompletedClick = { navController.navigate(Routes.ARCHIVE) },
                    onOverdueClick = { navigateBoard(OVERDUE_FILTER) }
                )
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
                    // 切换 tab 时立即确认撤销
                    mainViewModel.clearUndo()
                    if (tab == DockTab.BOARD) {
                        // 主页 tab 无条件回到「完整列表」：
                        // 带 filter 的主屏 entry 会被重建掉，避免
                        // 「currentRoute 已是 board 但页面还停在筛选态」导致点击像没反应。
                        navigateBoard(null)
                    } else if (currentRoute.toDockTab() == tab) {
                        // 点击当前非主页 tab：滚动到顶部
                        mainViewModel.requestScrollToTop()
                    } else {
                        navController.navigate(tab.route()) {
                            popUpTo(Routes.BOARD) {
                                saveState = true
                            }
                            launchSingleTop = true
                            // 不恢复各 tab 的历史状态：board 的历史 entry 上刻着统计页传进来的
                            // filter 路由参数，restoreState = true 会把它一并恢复，
                            // 导致"清掉的筛选复活 / 返回键失效 / 主页点不动"。
                            restoreState = false
                        }
                    }
                }
                // 注意：这里不再加 background / windowInsetsPadding。
                // 之前 Dock 会在整个导航栏内边距区域画一整块不透明深色底（浅色主题下就是一条黑条），
                // 切换标签时这一块会闪一下。现在背景只画在 DockView 自己的圆角卡片上，
                // 底部 insets 也由 DockView 内部通过 windowInsetsPadding 处理。
            )
        }

        // --------------------------------------------- FAB + 上半圆 Radial Menu
        // FAB 在屏幕底部水平居中、Dock 上方 24dp；菜单与遮罩都由它托管
        val fabShouldShow = showChrome && isBoard && !searchExpanded && fabVisibleByScroll
        val radialOpen = radialMenuState != null

        AnimatedVisibility(
            visible = fabShouldShow || radialOpen,
            enter = fadeIn(animationSpec = tween(200, easing = GentleEasing)) +
                scaleIn(initialScale = 0.8f, animationSpec = tween(220, easing = GentleEasing)),
            exit = fadeOut(animationSpec = tween(150, easing = GentleEasing)) +
                scaleOut(targetScale = 0.8f, animationSpec = tween(160, easing = GentleEasing)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(2f)
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = fabBottomPadding)
                    .windowInsetsPadding(windowInsets)
                    // 先测量 FAB 中心，供 Radial Menu 定位
                    .onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInWindow()
                        fabCenterX = bounds.center.x
                        fabCenterY = bounds.center.y
                    }
            ) {
                // 占位：真正的 FAB 由 RadialMenuHost 渲染（保证层级与遮罩正确）
                Box(modifier = Modifier.size(FabSize))
            }
        }

        // RadialMenuHost 负责遮罩 + 四个象限按钮 + FAB，覆盖全屏
        if (fabShouldShow || radialOpen) {
            RadialMenuHost(
                centerX = fabCenterX,
                centerY = fabCenterY,
                open = radialOpen,
                onToggle = {
                    container.radialMenuState.value =
                        if (radialOpen) null else RadialMenuState(fabCenterX, fabCenterY)
                },
                onSelect = { quadrant ->
                    container.radialMenuState.value = null
                    navController.navigate(Routes.edit(-1L, quadrant.name))
                },
                onDismiss = { container.radialMenuState.value = null }
            )
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
