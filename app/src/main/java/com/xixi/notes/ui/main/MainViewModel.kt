package com.xixi.notes.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xixi.notes.data.local.TaskEntity
import com.xixi.notes.data.preferences.AppPreferences
import com.xixi.notes.data.repository.TaskRepository
import com.xixi.notes.di.NavEvent
import com.xixi.notes.reminder.NotificationAction
import com.xixi.notes.reminder.TaskReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** HIGHLIGHT 定位目标 */
data class HighlightTarget(val taskId: Long, val nonce: Long)

/** 根级 UI 状态 */
data class AppUiState(
    /** 归档页需要高亮的任务 id */
    val highlightTaskId: Long? = null,
    /** 触发高亮动画的序号（同一任务重复高亮也会重新触发） */
    val highlightNonce: Long = 0L,
    /** 底部 Dock 当前标签 */
    val dockTab: String = "board"
)

/** 全局唯一的撤销槽位 */
data class UndoSlot(
    val token: Long,
    val message: String,
    /** 撤销时恢复的任务（删除撤销用） */
    val restore: TaskEntity? = null,
    /** 撤销时是否要清掉待执行的图片删除 */
    val cancelImageDeletionTaskId: Long? = null,
    /** 撤销的完成状态（完成 -> 撤销则为取消完成） */
    val uncheckTaskId: Long? = null
)

/** 一次性的界面提示（替代 Snackbar） */
data class BoardToast(val id: Long, val message: String)

class MainViewModel(
    private val repository: TaskRepository,
    private val preferences: AppPreferences,
    private val reminderScheduler: TaskReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _undo = MutableStateFlow<UndoSlot?>(null)
    val undo: StateFlow<UndoSlot?> = _undo.asStateFlow()

    private val _toast = MutableStateFlow<BoardToast?>(null)
    val toast: StateFlow<BoardToast?> = _toast.asStateFlow()

    /** 顶部栏状态：Dock 点击当前 tab 时自增，Board 监听后滚动到顶部 */
    private val _scrollToTopTick = MutableStateFlow(0)
    val scrollToTopTick: StateFlow<Int> = _scrollToTopTick.asStateFlow()

    private var tokenCounter = 0L
    private var nonceCounter = 0L

    /** 通知点击行为（打开设置时读取，变化时自动更新） */
    @Volatile
    var notificationAction: NotificationAction = NotificationAction.OPEN_EDIT
        private set

    init {
        viewModelScope.launch {
            preferences.prefs.collect { prefs ->
                notificationAction = NotificationAction.fromMode(prefs.notificationAction)
            }
        }
    }

    // ------------------------------------------------------------ 导航事件

    /**
     * 处理通知导航事件。返回需要打开的编辑页 taskId（null 表示不打开编辑页）。
     */
    fun consumeNavEvent(event: NavEvent, onOpenEditor: (Long) -> Unit) {
        viewModelScope.launch {
            val task = repository.getTaskById(event.taskId)
            if (task == null) {
                // 任务已删除：进主屏显示提示
                showToast("任务已被删除")
                return@launch
            }
            when (NotificationAction.fromString(event.action)) {
                NotificationAction.OPEN_EDIT -> onOpenEditor(task.id)
                NotificationAction.OPEN_HOME -> {
                    setDockTab("board")
                    highlight(task.id)
                }
                NotificationAction.HIGHLIGHT -> {
                    setDockTab("board")
                    highlight(task.id)
                }
            }
        }
    }

    // ------------------------------------------------------------ Dock

    fun setDockTab(tab: String) {
        _uiState.value = _uiState.value.copy(dockTab = tab)
    }

    /** 点击当前标签：滚动到顶部 */
    fun requestScrollToTop() {
        _scrollToTopTick.value = _scrollToTopTick.value + 1
    }

    // ------------------------------------------------------------ 高亮

    fun highlight(taskId: Long) {
        nonceCounter += 1
        _uiState.value = _uiState.value.copy(
            highlightTaskId = taskId,
            highlightNonce = nonceCounter
        )
    }

    fun clearHighlight() {
        _uiState.value = _uiState.value.copy(highlightTaskId = null)
    }

    // ------------------------------------------------------------ 撤销

    /** 完成 / 取消完成，并给出 3 秒撤销（全局只允许一个） */
    fun toggleCheck(task: TaskEntity) {
        viewModelScope.launch {
            val next = !task.isCheckedOff
            repository.setCheckedOff(task.id, next)
            if (next) {
                // 任务完成：取消已有提醒
                runCatching { reminderScheduler.cancel(task.id) }
                pushUndo(
                    UndoSlot(
                        token = ++tokenCounter,
                        message = "已完成「${task.title}」",
                        uncheckTaskId = task.id
                    )
                )
            } else {
                clearUndo()
            }
        }
    }

    /**
     * 非重要事项长按：延迟删除 + 撤销。
     *
     * 3 秒后才真正删除图片；应用被杀时任务保留、图片不删。
     */
    fun deleteWithUndo(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTaskRecord(task)
            pushUndo(
                UndoSlot(
                    token = ++tokenCounter,
                    message = "已删除「${task.title}」",
                    restore = task,
                    cancelImageDeletionTaskId = task.id
                )
            )
        }
    }

    /** 归档页永久删除：立即删除记录与图片 */
    fun deletePermanently(task: TaskEntity) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun getTask(taskId: Long, onResult: (TaskEntity?) -> Unit) {
        viewModelScope.launch { onResult(repository.getTaskById(taskId)) }
    }
    /**
     * 撤销当前槽位。
     *
     * @param onCancelImageDeletion 若为延迟删除，取消待执行的图片删除
     * @param onRestoreImages 恢复图片（延迟删除场景图片从未删除，无需处理）
     */
    fun undo(
        onCancelImageDeletion: (Long) -> Unit = {},
        onCompleted: () -> Unit = {}
    ) {
        val slot = _undo.value ?: return
        viewModelScope.launch {
            slot.restore?.let { repository.restoreTask(it) }
            slot.cancelImageDeletionTaskId?.let { onCancelImageDeletion(it) }
            slot.uncheckTaskId?.let { repository.setCheckedOff(it, false) }
            clearUndo()
            onCompleted()
        }
    }

    fun pushUndo(slot: UndoSlot) {
        _undo.value = slot
    }

    fun clearUndo() {
        _undo.value = null
    }

    // ------------------------------------------------------------ 提示

    fun showToast(message: String) {
        tokenCounter += 1
        _toast.value = BoardToast(tokenCounter, message)
    }

    fun dismissToast() {
        _toast.value = null
    }

    class Factory(
        private val repository: TaskRepository,
        private val preferences: AppPreferences,
        private val reminderScheduler: TaskReminderScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repository, preferences, reminderScheduler) as T
    }
}
