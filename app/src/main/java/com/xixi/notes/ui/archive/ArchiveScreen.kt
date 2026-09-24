package com.xixi.notes.ui.archive

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xixi.notes.R
import com.xixi.notes.di.LocalAppContainer
import com.xixi.notes.ui.components.InlineConfirm
import com.xixi.notes.ui.components.SeekSearchBar
import com.xixi.notes.ui.components.clickableNoRipple
import com.xixi.notes.ui.theme.Spacing
import com.xixi.notes.ui.theme.XixiTextStyles
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.GentleEasing
import kotlinx.coroutines.delay

/**
 * 归档页：所有已完成任务，倒序排列，支持搜索 / 取消完成 / 永久删除。
 *
 * 视觉：与主屏同一套卡片规范；空状态带 Compose 自绘动画（淡入 + 缩放 + 容器呼吸）。
 * 说明：项目内没有任何 Lottie 资源文件（res/raw、assets 均为空），因此按约定
 * 「找不到合适文件可用 Compose 模拟」处理，不伪造 Lottie JSON。
 */
@Composable
fun ArchiveScreen(
    highlightedTaskId: Long?,
    highlightNonce: Long,
    onOpenEditor: (Long) -> Unit,
    onOpenImageViewer: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchiveViewModel = viewModel(
        factory = ArchiveViewModel.Factory(
            LocalAppContainer.current.taskRepository,
            LocalAppContainer.current.reminderScheduler
        )
    )
) {
    val container = LocalAppContainer.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var searchExpanded by remember { mutableStateOf(false) }
    var confirmingTaskId by remember { mutableStateOf<Long?>(null) }
    var highlightId by remember { mutableStateOf<Long?>(null) }
    val highlightAlpha by animateFloatAsState(
        targetValue = if (highlightId != null) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = GentleEasing),
        label = "archive_highlight_alpha"
    )

    // HIGHLIGHT：加载目标任务（可能未完成，由 VM 临时插入列表）
    LaunchedEffect(highlightedTaskId, highlightNonce) {
        if (highlightedTaskId != null) {
            viewModel.loadHighlight(highlightedTaskId)
            highlightId = highlightedTaskId
        }
    }

    // 滚动到目标 + 闪烁 1 秒
    LaunchedEffect(highlightId, state.tasks) {
        val target = highlightId ?: return@LaunchedEffect
        val index = state.tasks.indexOfFirst { it.id == target }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            delay(1_000L)
            highlightId = null
            viewModel.loadHighlight(null)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XixiTheme.colors.background)
            .systemBarsPadding()
    ) {
        // 顶部栏：标题 + 右侧搜索图标
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = Spacing.screenH),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.archive_title),
                color = XixiTheme.colors.textPrimary,
                style = XixiTextStyles.pageTitle
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickableNoRipple { searchExpanded = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.cd_search),
                    tint = XixiTheme.colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 独立 Seek 搜索框
        if (searchExpanded) {
            SeekSearchBar(
                value = state.query,
                onValueChange = viewModel::setQuery,
                onClose = {
                    viewModel.setQuery("")
                    searchExpanded = false
                },
                expanded = true,
                onExpandRequest = { searchExpanded = true },
                modifier = Modifier.padding(start = Spacing.screenH, bottom = Spacing.sm)
            )
        }

        when {
            state.isEmpty -> ArchiveEmpty()
            state.noSearchResult -> ArchiveNoResult(onClear = { viewModel.setQuery("") })
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = Spacing.xs,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
                ) {
                    itemsIndexed(
                        items = state.tasks,
                        key = { _, task -> task.id }
                    ) { _, task ->
                        Column {
                            ArchiveRow(
                                task = task,
                                highlightAlpha = if (highlightId == task.id) highlightAlpha else 0f,
                                onUncomplete = { viewModel.uncomplete(task) },
                                onDelete = { confirmingTaskId = task.id },
                                onClick = {
                                    if (task.imagePaths.isNotEmpty()) {
                                        val index = container.imageViewerBridge.open(task.imagePaths, 0)
                                        onOpenImageViewer(index)
                                    } else {
                                        onOpenEditor(task.id)
                                    }
                                }
                            )
                            // 永久删除确认（inline confirm）
                            InlineConfirm(
                                visible = confirmingTaskId == task.id,
                                title = stringResource(R.string.archive_delete_title),
                                message = stringResource(R.string.archive_delete_message),
                                confirmLabel = stringResource(R.string.action_delete),
                                cancelLabel = stringResource(R.string.action_cancel),
                                onConfirm = {
                                    confirmingTaskId = null
                                    viewModel.deletePermanently(task)
                                },
                                onCancel = { confirmingTaskId = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 归档空状态（Compose 自绘动画）。
 *
 * 三层动画叠加，避免"死"的空页面：
 * 1. 入场：0.92 -> 1.0 缩放 + 淡入
 * 2. 图标容器呼吸：0.96 <-> 1.06 往复
 * 3. 图标下方柔和光晕随呼吸同步扩散
 */
@Composable
private fun ArchiveEmpty() {
    // 入场动画
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val enterScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.92f,
        animationSpec = tween(durationMillis = 360, easing = GentleEasing),
        label = "archive_empty_enter_scale"
    )
    val enterAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = GentleEasing),
        label = "archive_empty_enter_alpha"
    )
    // 呼吸动画
    val infinite = rememberInfiniteTransition(label = "archive_empty_breath")
    val breath by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = GentleEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "archive_empty_breath_value"
    )
    // 光晕：与呼吸反相，形成"呼吸感"
    val haloAlpha by infinite.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = GentleEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "archive_empty_halo"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.huge),
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
            // 图标 + 光晕
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(XixiTheme.colors.accent.copy(alpha = haloAlpha))
                        .graphicsLayer {
                            scaleX = breath
                            scaleY = breath
                        }
                )
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = XixiTheme.colors.accent,
                    modifier = Modifier
                        .size(44.dp)
                        .graphicsLayer {
                            scaleX = breath
                            scaleY = breath
                        }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
            Text(
                text = stringResource(R.string.archive_empty),
                color = XixiTheme.colors.textPrimary,
                style = XixiTextStyles.subtitle,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.stats_empty_hint),
                color = XixiTheme.colors.textSecondary,
                style = XixiTextStyles.caption,
                textAlign = TextAlign.Center
            )
        }
    }
}
