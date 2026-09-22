package com.xixi.notes.ui.board

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xixi.notes.R
import com.xixi.notes.data.preferences.AppPrefs
import com.xixi.notes.di.LocalAppContainer
import com.xixi.notes.ui.components.EmptyGroupHint
import com.xixi.notes.ui.components.GroupHeaderRow
import com.xixi.notes.ui.components.InlineConfirm
import com.xixi.notes.ui.components.LiqSortButton
import com.xixi.notes.ui.components.LiqSortMenuOverlay
import com.xixi.notes.ui.components.SeekSearchBar
import com.xixi.notes.ui.components.SortOptionUi
import com.xixi.notes.ui.components.TaskRow
import com.xixi.notes.ui.components.TaskRowHeight
import com.xixi.notes.ui.components.clickableNoRipple
import com.xixi.notes.ui.components.colorOf
import com.xixi.notes.ui.main.AppUiState
import com.xixi.notes.ui.main.BoardToast
import com.xixi.notes.ui.main.MainViewModel
import com.xixi.notes.ui.main.OVERDUE_FILTER
import com.xixi.notes.ui.main.UndoSlot
import com.xixi.notes.ui.theme.Spacing
import com.xixi.notes.ui.theme.XixiElevation
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.theme.XixiThumbnailShape
import com.xixi.notes.ui.util.GentleEasing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** 排序菜单宽度（用于避让计算） */
private val SortButtonSize = 46.dp

/** 向下滚动累计超过该距离才隐藏 FAB */
private const val HIDE_THRESHOLD_PX = 50

/**
 * 主屏。
 *
 * 顶部栏：搜索 + 排序 + magnet-select + 「显示已完成」眼睛图标。
 * 列表：轻重缓急四分组（可折叠）或按截止日期 / 按创建时间平铺。
 *
 * 返回键（由本页 BackHandler 统一处理，优先级高于根 Scaffold 的退出逻辑）：
 * 1. 收起 Inline Confirm -> 2. 收起排序菜单 -> 3. 收起搜索（含清空关键词）
 * -> 4. 有筛选：去掉 filter 路由参数、原地重建为完整列表 -> 5. 无筛选：退出应用
 */
@Composable
fun BoardScreen(
    mainViewModel: MainViewModel,
    appState: AppUiState,
    undo: UndoSlot?,
    toast: BoardToast?,
    scrollToTopTick: Int,
    listState: LazyListState,
    onOpenEditor: (Long) -> Unit,
    onOpenImageViewer: (Int) -> Unit,
    onSearchExpandedChange: (Boolean) -> Unit,
    onScrollVisibilityChange: (Boolean) -> Unit,
    /**
     * 清掉筛选，由 MainScaffold 负责导航；本页不做任何筛选状态回写。
     *
     * @param returnToStats true：返回键 —— 清筛选并回到统计页（本次从统计页进来的话）；
     *   false：筛选条 ✕ —— 只清筛选，留在主屏看完整列表。
     */
    onClearFilter: (returnToStats: Boolean) -> Unit,
    /** 返回键且「无筛选」时：退出应用（退到后台） */
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier,
    /** 从统计页传入的筛选参数（象限名或 OVERDUE），null 表示不筛选 */
    initialFilter: String? = null,
    viewModel: BoardViewModel = viewModel(
        factory = BoardViewModel.Factory(
            LocalAppContainer.current.taskRepository,
            LocalAppContainer.current.preferences
        )
    )
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs by container.preferences.prefs.collectAsStateWithLifecycle(
        initialValue = AppPrefs()
    )

    // 筛选的唯一来源：本页的路由参数 board?filter={filter}。
    //
    // key 用 Unit：路由参数在本次 entry 生命周期内不会改变，只有"进入主屏"这一次需要解析。
    // 不回写任何持久化存储（原先的 SavedStateHandle board_filter_token 已删除），
    // 所以用户清掉筛选后不会被重新写回，也就不会出现"筛选标签清不掉 / 返回键失效"。
    val currentInitialFilter by rememberUpdatedState(initialFilter)
    LaunchedEffect(Unit) {
        viewModel.setRouteFilter(parseFilterArg(currentInitialFilter))
    }

    // 排序菜单与确认态
    var sortMenuOpen by remember { mutableStateOf(false) }
    // 排序按钮右下角锚点（窗口坐标），供面板定位
    var sortAnchorX by remember { mutableStateOf(0f) }
    var sortAnchorY by remember { mutableStateOf(0f) }
    var confirmingTaskId by remember { mutableStateOf<Long?>(null) }

    // 高亮闪烁
    var highlightTaskId by remember { mutableStateOf<Long?>(null) }
    val highlightAlpha by animateFloatAsState(
        targetValue = if (highlightTaskId != null) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = GentleEasing),
        label = "board_highlight"
    )

    // 离开主屏时复位搜索 / 临时显示 / 折叠展开
    LaunchedEffect(state.searchExpanded) {
        onSearchExpandedChange(state.searchExpanded)
    }

    // 滚动方向 -> FAB 显隐：
    // - 列表不可滚动（内容不足一屏）时强制保持可见
    // - 仅向下滚动累计超过 50dp 才隐藏
    // - 任何向上滚动立即显示
    // - 回到顶部/离开屏幕时复位为可见
    DisposableEffect(listState) {
        val job = scope.launch {
            var lastOffset = 0
            var hiddenAccum = 0
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .distinctUntilChanged()
                .collect { (index, offset) ->
                    val canScroll = listState.canScrollForward || listState.canScrollBackward
                    if (!canScroll) {
                        // 不可滚动：强制可见
                        hiddenAccum = 0
                        lastOffset = 0
                        onScrollVisibilityChange(true)
                        return@collect
                    }
                    val current = index * 100_000 + offset
                    val delta = current - lastOffset
                    lastOffset = current
                    when {
                        delta < 0 -> {
                            // 向上滚动：立即显示
                            hiddenAccum = 0
                            onScrollVisibilityChange(true)
                        }
                        delta > 0 -> {
                            // 向下滚动：累计超过阈值才隐藏
                            hiddenAccum += delta
                            if (hiddenAccum >= HIDE_THRESHOLD_PX) {
                                onScrollVisibilityChange(false)
                            }
                        }
                    }
                }
        }
        onDispose {
            job.cancel()
            onScrollVisibilityChange(true)
        }
    }

    // 点击当前 Dock 标签：滚动到顶部
    LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick > 0) {
            runCatching { listState.animateScrollToItem(0) }
        }
    }

    // HIGHLIGHT：展开折叠分组（300ms 平滑）-> 滚动 -> 闪烁 1 秒
    LaunchedEffect(appState.highlightTaskId, appState.highlightNonce) {
        val target = appState.highlightTaskId ?: return@LaunchedEffect
        viewModel.setTransientVisible(target)

        // 等待列表刷新后重新读取，保证定位准确
        var rows = state.rows
        if (rows.none { it is BoardRow.TaskRowItem && it.task.id == target }) {
            delay(120L)
            rows = viewModel.uiState.value.rows
        }

        val quadrant = rows.filterIsInstance<BoardRow.TaskRowItem>()
            .firstOrNull { it.task.id == target }
            ?.quadrant
            ?: rows.filterIsInstance<BoardRow.GroupHeader>()
                .firstOrNull { header -> header.group.tasks.any { it.id == target } }
                ?.group?.quadrant

        if (quadrant != null && viewModel.uiState.value.foldedGroups[quadrant.groupKey] == true) {
            // 先平滑展开折叠分组（animateContentSize 300ms），再滚动
            viewModel.toggleFolded(quadrant)
            delay(340L)
        }

        val index = viewModel.uiState.value.rows.indexOfFirst { row ->
            row is BoardRow.TaskRowItem && row.task.id == target
        }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
        highlightTaskId = target
        delay(1_000L)
        highlightTaskId = null
        mainViewModel.clearHighlight()
    }

    // 离开主屏：恢复 HIDDEN 与折叠状态
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearTransientVisible()
            viewModel.onSearchStateReset()
        }
    }

    // 返回键优先级：确认框 -> 排序菜单 -> 搜索 -> 清筛选回统计页 -> 退出应用
    BackHandler(
        enabled = state.searchExpanded ||
            sortMenuOpen ||
            confirmingTaskId != null ||
            state.filterActive
    ) {
        when {
            confirmingTaskId != null -> confirmingTaskId = null
            sortMenuOpen -> sortMenuOpen = false
            state.searchExpanded -> viewModel.closeSearch()
            // 有筛选：清筛选；本次是从统计页筛选进来的话，顺带退回统计页
            state.filterActive -> onClearFilter(true)
            // 没有筛选：退出应用（退到后台，不销毁任务状态）
            else -> onExitApp()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XixiTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // ---------------------------------------------------------- 顶部栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(start = Spacing.lg, end = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SeekSearchBar(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    onClose = { viewModel.closeSearch() },
                    expanded = state.searchExpanded,
                    onExpandRequest = { viewModel.openSearch() },
                    modifier = Modifier.wrapContentWidth()
                )

                Spacer(modifier = Modifier.weight(1f))

                // 搜索展开时其他按钮淡出 + 缩小
                val othersAlpha by animateFloatAsState(
                    targetValue = if (state.searchExpanded) 0f else 1f,
                    animationSpec = tween(durationMillis = 220, easing = GentleEasing),
                    label = "topbar_others_alpha"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier
                        .alpha(othersAlpha)
                        .graphicsLayer {
                            scaleX = 1f - 0.15f * (1f - othersAlpha)
                            scaleY = 1f - 0.15f * (1f - othersAlpha)
                        }
                ) {
                    // 排序（46dp 圆形图标）：面板不在这里渲染，见根部 LiqSortMenuOverlay
                    Box(modifier = Modifier.size(SortButtonSize)) {
                        LiqSortButton(
                            open = sortMenuOpen,
                            onToggle = { sortMenuOpen = !sortMenuOpen },
                            onAnchor = { left, top, width, height ->
                                sortAnchorX = left + width
                                sortAnchorY = top + height
                            }
                        )
                    }

                    // 眼睛图标：展开 / 收起全部分类（状态同步 DataStore 的 foldedGroups）
                    val allFolded = state.allGroupsFolded
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickableNoRipple { viewModel.toggleAllFolded() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (allFolded) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = stringResource(
                                if (allFolded) R.string.cd_expand_all_groups
                                else R.string.cd_collapse_all_groups
                            ),
                            tint = XixiTheme.colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 统计页传入的筛选条件：显示可清除的筛选条
            if (state.filterActive) {
                FilterChipRow(
                    label = filterLabel(state.filter),
                    // 筛选条 ✕：只清筛选，留在主屏
                    onClear = { onClearFilter(false) }
                )
            }

            // ------------------------------------------------------------ 列表
            if (state.isEmpty) {
                EmptyBoard(searching = state.searching) {
                    viewModel.setQuery("")
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(confirmingTaskId) {
                            if (confirmingTaskId != null) {
                                // 点击任意位置收起 inline confirm（事件仍继续传递）
                                awaitEachGesture {
                                    awaitFirstDown(
                                        requireUnconsumed = false,
                                        pass = PointerEventPass.Initial
                                    )
                                    confirmingTaskId = null
                                }
                            }
                        },
                    contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp)
                ) {
                    state.rows.forEach { row ->
                        when (row) {
                            is BoardRow.GroupHeader -> item(key = row.key) {
                                Column {
                                    GroupHeaderRow(
                                        title = stringResource(row.group.quadrant.titleRes),
                                        count = row.count,
                                        color = XixiTheme.quadrant.colorOf(row.group.quadrant),
                                        folded = row.group.isFolded,
                                        onToggleFold = {
                                            viewModel.toggleFolded(row.group.quadrant)
                                        }
                                    )
                                    // 空分类展开时给出提示，保证四个分类结构始终完整
                                    if (row.count == 0 && !row.group.isFolded) {
                                        EmptyGroupHint()
                                    }
                                }
                            }

                            is BoardRow.TaskRowItem -> item(key = row.key) {
                                // 折叠时行高平滑收缩至 0，只保留分组头部
                                val isFolded = state.foldedGroups[row.quadrant.groupKey] == true
                                val itemHeight by animateDpAsState(
                                    targetValue = if (isFolded) 0.dp else TaskRowHeight + 8.dp,
                                    animationSpec = tween(300, easing = GentleEasing),
                                    label = "row_height"
                                )
                                val itemAlpha by animateFloatAsState(
                                    targetValue = if (isFolded) 0f else 1f,
                                    animationSpec = tween(220, easing = GentleEasing),
                                    label = "row_alpha"
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(itemHeight)
                                        .alpha(itemAlpha)
                                        .clipToBounds()
                                ) {
                                    TaskRow(
                                        task = row.task,
                                        quadrant = row.quadrant,
                                        highlightAlpha = if (highlightTaskId == row.task.id) {
                                            highlightAlpha
                                        } else {
                                            0f
                                        },
                                        // 左圆圈：完成 / 取消完成（3 秒内可撤销）
                                        onToggleCheck = {
                                            mainViewModel.toggleCheck(row.task)
                                        },
                                        // 右叉号：非重要直接删除 + 撤销，重要先原地确认
                                        onDelete = {
                                            if (row.task.isImportant) {
                                                confirmingTaskId = row.task.id
                                            } else {
                                                mainViewModel.deleteWithUndo(row.task)
                                            }
                                        },
                                        onClick = {
                                            confirmingTaskId = null
                                            onOpenEditor(row.task.id)
                                        },
                                        onLongPress = {
                                            // 长按保留为删除快捷入口，规则与叉号一致
                                            if (row.task.isImportant) {
                                                confirmingTaskId = row.task.id
                                            } else {
                                                mainViewModel.deleteWithUndo(row.task)
                                            }
                                        }
                                    )

                                    // 重要事项长按的 Inline Confirm
                                    InlineConfirm(
                                        visible = confirmingTaskId == row.task.id,
                                        title = stringResource(R.string.confirm_delete_title),
                                        message = stringResource(R.string.confirm_delete_message),
                                        confirmLabel = stringResource(R.string.action_delete),
                                        cancelLabel = stringResource(R.string.action_cancel),
                                        onConfirm = {
                                            confirmingTaskId = null
                                            viewModel.deleteNow(row.task)
                                        },
                                        onCancel = { confirmingTaskId = null }
                                    )

                                    // 撤销条（紧贴该行下方）：完成 / 取消完成的撤销
                                    AnimatedVisibility(
                                        visible = undo != null &&
                                            undo.checkUndoTaskId == row.task.id,
                                        enter = fadeIn(tween(180, easing = GentleEasing)),
                                        exit = fadeOut(tween(140, easing = GentleEasing))
                                    ) {
                                        UndoInline(
                                            message = undo?.message.orEmpty(),
                                            onUndo = {
                                                mainViewModel.undo(
                                                    onCancelImageDeletion = { taskId ->
                                                        viewModel.deleteTaskImages(taskId)
                                                    }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 底部撤销条：仅用于删除撤销（任务已不在列表里，无法贴在行下）
        // 完成 / 取消完成的撤销是行内条（见上方 AnimatedVisibility）
        AnimatedVisibility(
            visible = undo != null && undo.checkUndoTaskId == null,
            enter = fadeIn(tween(180, easing = GentleEasing)) +
                scaleIn(initialScale = 0.96f, animationSpec = tween(200, easing = GentleEasing)),
            exit = fadeOut(tween(140, easing = GentleEasing)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 152.dp)
        ) {
            UndoInline(
                message = undo?.message.orEmpty(),
                onUndo = {
                    mainViewModel.undo(
                        onCancelImageDeletion = { taskId ->
                            viewModel.deleteTaskImages(taskId)
                        }
                    )
                }
            )
        }

        // 通知点击已删除任务时的提示
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(tween(180, easing = GentleEasing)) +
                scaleIn(initialScale = 0.96f, animationSpec = tween(200, easing = GentleEasing)),
            exit = fadeOut(tween(160, easing = GentleEasing)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
        ) {
            ToastInline(
                message = toast?.message.orEmpty(),
                onDismiss = { mainViewModel.dismissToast() }
            )
        }

        // 排序菜单：在根部全屏层渲染，绝对定位贴在排序按钮下方，
        // 避免被固定 56dp 的顶部栏约束夹扁
        LiqSortMenuOverlay(
            anchorX = sortAnchorX,
            anchorY = sortAnchorY,
            open = sortMenuOpen,
            options = sortOptions(prefs.sortMode),
            onSelect = { id ->
                // 先关闭菜单，再写 DataStore（ViewModel 会回推并触发重新排序）
                sortMenuOpen = false
                runCatching { viewModel.setSortMode(SortMode.valueOf(id)) }
            },
            onDismiss = { sortMenuOpen = false }
        )
    }

    // 提示 3 秒后自动消失
    LaunchedEffect(toast?.id) {
        if (toast != null) {
            delay(3_000L)
            mainViewModel.dismissToast()
        }
    }

    // 高亮任务离开主屏后恢复隐藏
    LaunchedEffect(appState.highlightTaskId) {
        if (appState.highlightTaskId == null) {
            viewModel.clearTransientVisible()
        }
    }
}

/** 排序菜单选项：轻重缓急排序 / 按截止日期 / 按创建时间 */
@Composable
private fun sortOptions(current: SortMode): List<SortOptionUi> = listOf(
    SortOptionUi(
        id = SortMode.PRIORITY.name,
        label = stringResource(R.string.sort_priority),
        selected = current == SortMode.PRIORITY
    ),
    SortOptionUi(
        id = SortMode.DUE_DATE.name,
        label = stringResource(R.string.sort_due_date),
        selected = current == SortMode.DUE_DATE
    ),
    SortOptionUi(
        id = SortMode.CREATED_AT.name,
        label = stringResource(R.string.sort_created_at),
        selected = current == SortMode.CREATED_AT
    )
)

/** 路由参数（象限名或 OVERDUE）-> 筛选条件 */
private fun parseFilterArg(raw: String?): BoardFilter = when (raw) {
    null -> BoardFilter.None
    OVERDUE_FILTER -> BoardFilter.Overdue
    else -> runCatching { Quadrant.valueOf(raw) }
        .getOrNull()
        ?.let { BoardFilter.QuadrantOnly(it) }
        ?: BoardFilter.None
}

/** 筛选条文案 */
@Composable
private fun filterLabel(filter: BoardFilter): String = when (filter) {
    is BoardFilter.QuadrantOnly -> stringResource(
        R.string.filter_label_quadrant,
        stringResource(filter.quadrant.titleRes)
    )
    BoardFilter.Overdue -> stringResource(R.string.filter_label_overdue)
    BoardFilter.None -> ""
}

/** 当前筛选条件（可一键清除）：圆角小卡片 + 柔和阴影 */
@Composable
private fun FilterChipRow(
    label: String,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .shadow(XixiElevation.card, XixiThumbnailShape)
                .clip(XixiThumbnailShape)
                .background(XixiTheme.colors.card)
                .padding(start = Spacing.md, end = Spacing.xs, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = XixiTheme.colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickableNoRipple(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.filter_clear),
                    tint = XixiTheme.colors.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/** 行内撤销条：圆角小卡片；「撤销」使用统一强调色 */
@Composable
private fun UndoInline(
    message: String,
    onUndo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            .shadow(XixiElevation.card, XixiThumbnailShape)
            .clip(XixiThumbnailShape)
            .background(XixiTheme.colors.card)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = XixiTheme.colors.textSecondary,
            fontSize = 13.sp,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(XixiTheme.shapes.small)
                .clickableNoRipple(onClick = onUndo)
                .padding(horizontal = Spacing.md, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.action_undo),
                // 操作类文字统一使用强调色
                color = XixiTheme.colors.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

/** 顶部提示条：圆角小卡片 + 柔和阴影 */
@Composable
private fun ToastInline(
    message: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = Spacing.lg)
            .shadow(XixiElevation.cardStrong, XixiTheme.shapes.item)
            .clip(XixiTheme.shapes.item)
            .background(XixiTheme.colors.card)
            .clickableNoRipple(onClick = onDismiss)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = XixiTheme.colors.textPrimary,
            fontSize = 13.sp,
            letterSpacing = 0.3.sp
        )
    }
}

/** 主屏空状态（Compose 自绘动画：淡入 + 放大 + 呼吸，不引入第三方动画库） */
@Composable
private fun EmptyBoard(
    searching: Boolean,
    onClear: () -> Unit
) {
    // 入场动画：0.92 -> 1.0 并淡入
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val enterScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "empty_enter_scale"
    )
    val enterAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = GentleEasing),
        label = "empty_enter_alpha"
    )
    // 呼吸动画：图标缓慢起伏
    val infinite = rememberInfiniteTransition(label = "empty_breath")
    val breath by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = GentleEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "empty_breath_value"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = enterScale
                scaleY = enterScale
                alpha = enterAlpha
            }
        ) {
            if (!searching) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    // 空状态图标使用统一强调色
                    tint = XixiTheme.colors.accent.copy(alpha = 0.75f),
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer {
                            scaleX = breath
                            scaleY = breath
                        }
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
            }
            Text(
                text = stringResource(
                    if (searching) R.string.search_no_result else R.string.board_empty_title
                ),
                color = XixiTheme.colors.textSecondary,
                fontSize = 15.sp,
                letterSpacing = 0.3.sp,
                textAlign = TextAlign.Center
            )
            if (searching) {
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
                        fontSize = 13.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}
