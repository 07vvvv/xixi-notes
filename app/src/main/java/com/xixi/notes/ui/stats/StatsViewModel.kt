package com.xixi.notes.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xixi.notes.data.local.TaskEntity
import com.xixi.notes.data.repository.TaskRepository
import com.xixi.notes.ui.board.Quadrant
import com.xixi.notes.ui.util.isCompletedThisWeek
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 统计页数据 */
data class StatsUiState(
    val counts: Map<Quadrant, Int> = emptyMap(),
    val weekCompleted: Int = 0,
    val totalCompleted: Int = 0,
    val overdue: Int = 0,
    val totalTasks: Int = 0
) {
    val isEmpty: Boolean get() = totalTasks == 0
}

class StatsViewModel(private val repository: TaskRepository) : ViewModel() {

    /** 每分钟刷新一次「逾期」统计 */
    private val nowTick = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                nowTick.value = System.currentTimeMillis()
            }
        }
    }

    val uiState: StateFlow<StatsUiState> = combine(
        repository.allTasks,
        nowTick
    ) { tasks, now ->
        compute(tasks, now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )

    private fun compute(tasks: List<TaskEntity>, now: Long): StatsUiState {
        val counts = Quadrant.ordered.associateWith { quadrant ->
            tasks.count { Quadrant.of(it) == quadrant }
        }
        val completed = tasks.filter { it.isCheckedOff }
        return StatsUiState(
            counts = counts,
            // 本周完成：completedAt 在 ISO 周（周一~周日）区间内
            weekCompleted = completed.count { isCompletedThisWeek(it) },
            totalCompleted = completed.size,
            overdue = tasks.count {
                !it.isCheckedOff && it.dueDate != null && it.dueDate < now
            },
            totalTasks = tasks.size
        )
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatsViewModel(repository) as T
    }
}
