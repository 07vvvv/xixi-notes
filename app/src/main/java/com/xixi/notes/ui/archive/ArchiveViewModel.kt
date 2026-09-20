package com.xixi.notes.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xixi.notes.data.local.TaskEntity
import com.xixi.notes.data.repository.TaskRepository
import com.xixi.notes.reminder.TaskReminderScheduler
import com.xixi.notes.ui.util.archiveComparator
import com.xixi.notes.ui.util.matchesQuery
import com.xixi.notes.ui.util.searchRelevance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 归档页 UI 状态 */
data class ArchiveUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val query: String = "",
    val hasAnyArchived: Boolean = false
) {
    val noSearchResult: Boolean get() = query.isNotBlank() && tasks.isEmpty()
    val isEmpty: Boolean get() = hasAnyArchived.not() && query.isBlank()
}

class ArchiveViewModel(
    private val repository: TaskRepository,
    private val reminderScheduler: TaskReminderScheduler
) : ViewModel() {

    private val query = MutableStateFlow("")

    /** HIGHLIGHT 目标：归档页临时插入的任务（可能未完成） */
    private val highlightTask = MutableStateFlow<TaskEntity?>(null)

    val uiState: StateFlow<ArchiveUiState> = combine(
        repository.allTasks,
        query,
        highlightTask
    ) { tasks, currentQuery, highlighted ->
        build(tasks, currentQuery, highlighted)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ArchiveUiState()
    )

    private fun build(
        tasks: List<TaskEntity>,
        currentQuery: String,
        highlighted: TaskEntity?
    ): ArchiveUiState {
        // 所有已完成的，按 completedAt ?: updatedAt 倒序
        val archived = tasks.filter { it.isCheckedOff }
        val withHighlight = if (highlighted != null && archived.none { it.id == highlighted.id }) {
            archived + highlighted
        } else {
            archived
        }

        val filtered = if (currentQuery.isBlank()) {
            withHighlight.sortedWith(archiveComparator())
        } else {
            // 搜索结果按相关度排序：标题完全 > 标题包含 > 描述包含
            withHighlight
                .filter { matchesQuery(it, currentQuery) }
                .sortedWith(
                    compareBy<TaskEntity> { searchRelevance(it, currentQuery) }
                        .thenByDescending { it.completedAt ?: it.updatedAt }
                )
        }

        return ArchiveUiState(
            tasks = filtered,
            query = currentQuery,
            hasAnyArchived = archived.isNotEmpty()
        )
    }

    fun setQuery(value: String) {
        query.value = value.take(120)
    }

    fun loadHighlight(taskId: Long?) {
        if (taskId == null) {
            highlightTask.value = null
            return
        }
        viewModelScope.launch {
            highlightTask.value = repository.getTaskById(taskId)
        }
    }

    fun uncomplete(task: TaskEntity) {
        viewModelScope.launch {
            repository.setCheckedOff(task.id, false)
            // 取消完成后不再需要原有提醒
            reminderScheduler.cancel(task.id)
        }
    }

    fun deletePermanently(task: TaskEntity) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    class Factory(
        private val repository: TaskRepository,
        private val reminderScheduler: TaskReminderScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ArchiveViewModel(repository, reminderScheduler) as T
    }
}
