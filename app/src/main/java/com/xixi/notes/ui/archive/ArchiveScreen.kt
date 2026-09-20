package com.xixi.notes.ui.archive

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xixi.notes.R
import com.xixi.notes.di.LocalAppContainer
import com.xixi.notes.ui.components.InlineConfirm
import com.xixi.notes.ui.components.SeekSearchBar
import com.xixi.notes.ui.components.clickableNoRipple
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.GentleEasing
import kotlinx.coroutines.delay

/** 归档页：所有已完成任务，倒序排列，支持搜索 / 取消完成 / 永久删除 */
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
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.archive_title),
                color = XixiTheme.colors.textPrimary,
                fontSize = 20.sp,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.SemiBold
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
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }

        when {
            state.isEmpty -> ArchiveEmpty()
            state.noSearchResult -> ArchiveNoResult(onClear = { viewModel.setQuery("") })
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
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

/** 归档空状态 */
@Composable
private fun ArchiveEmpty() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.archive_empty),
            color = XixiTheme.colors.textSecondary,
            fontSize = 15.sp,
            letterSpacing = 0.5.sp
        )
    }
}
