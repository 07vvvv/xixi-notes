package com.xixi.notes.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xixi.notes.data.local.TaskEntity
import com.xixi.notes.data.preferences.AppPrefs
import com.xixi.notes.data.preferences.AppPreferences
import com.xixi.notes.data.repository.TaskRepository
import com.xixi.notes.ui.theme.ThemeMode
import com.xixi.notes.ui.util.createdAtComparator
import com.xixi.notes.ui.util.dueDateComparator
import com.xixi.notes.ui.util.matchesQuery
import com.xixi.notes.ui.util.priorityGroupComparator
import com.xixi.notes.ui.util.priorityRank
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 列表中的一行（分组头或任务） */
sealed interface BoardRow {
    val key: String

    data class GroupHeader(val group: TaskQuadrantGroup, val count: Int) : BoardRow {
        override val key: String get() = "header_${group.quadrant.groupKey}"
    }

    data class TaskRowItem(
        val task: TaskEntity,
        val quadrant: Quadrant
    ) : BoardRow {
        override val key: String get() = "task_${task.id}"
    }
}

/** 主屏一次性事件 */
sealed interface BoardEvent {
    data object ScrolledToTop : BoardEvent
    data class Error(val message: String) : BoardEvent
}

/** 主屏筛选条件（由统计页点击传入） */
sealed interface BoardFilter {
    /** 单个象限 */
    data class QuadrantOnly(val quadrant: Quadrant) : BoardFilter

    /** 逾期任务 */
    data object Overdue : BoardFilter

    /** 没有筛选 */
    data object None : BoardFilter
}

/**
 * 筛选条件 [BoardFilter] 只有一个事实来源：主屏的路由参数 `board?filter={filter}`。
 *
 * 本 ViewModel **不再**持久化筛选（原先的 `board_filter_token` 已删除）：
 * 路由参数本身随返回栈保存与恢复，进程重建后由 `BoardScreen(initialFilter = ...)`
 * 重新解析并写回这里的内存态。少一份来源，就不会再出现"清掉的筛选被复活"。
 */

/** 主屏 UI 状态 */
data class BoardUiState(
    val rows: List<BoardRow> = emptyList(),
    val grouped: Boolean = true,
    val searchExpanded: Boolean = false,
    val query: String = "",
    val searching: Boolean = false,
    val foldedGroups: Map<String, Boolean> = emptyMap(),
    val hasAnyTask: Boolean = false,
    /** 当前生效的筛选条件（来自路由参数） */
    val filter: BoardFilter = BoardFilter.None,
    /** 是否全部四个分组都处于折叠状态（驱动眼睛图标） */
    val allGroupsFolded: Boolean = false
) {
    val isEmpty: Boolean get() = rows.isEmpty()
    val noSearchResult: Boolean get() = searching && rows.isEmpty()

    /** 是否处于筛选态（返回键据此决定"清筛选"还是"退到后台"） */
    val filterActive: Boolean get() = filter != BoardFilter.None
}

class BoardViewModel(
    private val repository: TaskRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    /** 搜索关键词（独立于 UI 状态，避免每次输入都重建分组） */
    private val query = MutableStateFlow("")

    /** 搜索框是否展开 */
    private val searchExpanded = MutableStateFlow(false)

    /**
     * 当前筛选条件（内存态）。
     *
     * 由 [BoardScreen] 在进入主屏时依据路由参数写入（[setRouteFilter]），
     * 用户清筛选时由路由参数变成 null 重新写入 —— 全程只有路由参数这一个来源。
     */
    private val routeFilter = MutableStateFlow<BoardFilter>(BoardFilter.None)

    /** HIGHLIGHT 定位时临时显示的任务 id（离开主屏复位） */
    private val transientVisible = MutableStateFlow<Set<Long>>(emptySet())

    /** 供 combine 使用的输入聚合体（kotlinx combine 最多支持 5 个流，这里先两两合并） */
    private data class BoardInputs(
        val tasks: List<TaskEntity>,
        val prefs: AppPrefs,
        val query: String,
        val expanded: Boolean
    )

    private val baseInputs = combine(
        repository.allTasks,
        preferences.prefs,
        query,
        searchExpanded
    ) { tasks, prefs, currentQuery, expanded ->
        BoardInputs(
            tasks = tasks,
            prefs = prefs,
            query = currentQuery,
            expanded = expanded
        )
    }

    private val extraInputs = combine(
        routeFilter,
        transientVisible
    ) { filter, transient ->
        filter to transient
    }

    val uiState: StateFlow<BoardUiState> = combine(
        baseInputs,
        extraInputs
    ) { base, extra ->
        buildState(
            tasks = base.tasks,
            prefs = base.prefs,
            currentQuery = base.query,
            expanded = base.expanded,
            filter = extra.first,
            transient = extra.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BoardUiState()
    )

    private fun buildState(
        tasks: List<TaskEntity>,
        prefs: AppPrefs,
        currentQuery: String,
        expanded: Boolean,
        filter: BoardFilter,
        transient: Set<Long>
    ): BoardUiState {
        val searching = expanded && currentQuery.isNotBlank()

        // 搜索：图片不参与，命中标题或描述
        val matched = if (searching) {
            tasks.filter { matchesQuery(it, currentQuery) }
        } else {
            tasks
        }

        // 筛选：直接使用路由参数解析出来的条件
        val filtered = applyFilter(matched, filter)

        // 已完成显示方式：HIDDEN 时排除，但高亮定位的任务临时可见
        val visible = when (prefs.completedStyle) {
            CompletedStyle.IN_PLACE -> filtered
            CompletedStyle.HIDDEN -> filtered.filter { !it.isCheckedOff || it.id in transient }
        }

        val rows = if (searching || prefs.sortMode != SortMode.PRIORITY) {
            // 搜索时切平铺列表；按截止日期 / 按创建时间同样平铺
            buildFlat(visible, prefs)
        } else {
            buildGrouped(visible, prefs)
        }
        val allFolded = Quadrant.ordered.all { prefs.foldedGroups[it.groupKey] == true }
        return BoardUiState(
            rows = rows,
            grouped = !searching && prefs.sortMode == SortMode.PRIORITY,
            searchExpanded = expanded,
            query = currentQuery,
            searching = searching,
            foldedGroups = prefs.foldedGroups,
            hasAnyTask = tasks.isNotEmpty(),
            filter = filter,
            allGroupsFolded = allFolded
        )
    }

    /** 应用统计页传入的筛选条件 */
    private fun applyFilter(tasks: List<TaskEntity>, filter: BoardFilter): List<TaskEntity> =
        when (filter) {
            is BoardFilter.QuadrantOnly -> tasks.filter { Quadrant.of(it) == filter.quadrant }
            BoardFilter.Overdue -> tasks.filter {
                !it.isCheckedOff && it.dueDate != null && it.dueDate < System.currentTimeMillis()
            }
            BoardFilter.None -> tasks
        }

    /**
     * 轻重缓急：四分组，组间按优先级，组内按创建时间正序（已移除 assignee 规则）。
     *
     * **四个分组始终全部存在**：即使分组内没有任务，也输出分组头部（名称 + 0 徽章 + 箭头），
     * 保证用户能看到完整的四象限结构。
     */
    private fun buildGrouped(tasks: List<TaskEntity>, prefs: AppPrefs): List<BoardRow> {
        val comparator = priorityGroupComparator()
        val rows = mutableListOf<BoardRow>()
        Quadrant.ordered.forEach { quadrant ->
            val groupTasks = tasks.filter { Quadrant.of(it) == quadrant }.sortedWith(comparator)
            val folded = prefs.foldedGroups[quadrant.groupKey] == true
            val group = TaskQuadrantGroup(
                quadrant = quadrant,
                tasks = groupTasks,
                isFolded = folded
            )
            rows += BoardRow.GroupHeader(group = group, count = groupTasks.size)
            // 折叠或空分组都只保留头部
            if (!folded && groupTasks.isNotEmpty()) {
                rows += buildTaskRows(groupTasks, quadrant)
            }
        }
        return rows
    }

    /** 按截止日期 / 按创建时间 / 搜索：平铺列表，按当前排序模式排序 */
    private fun buildFlat(tasks: List<TaskEntity>, prefs: AppPrefs): List<BoardRow> {
        val sorted = tasks.sortedWith(comparatorFor(prefs))
        return buildTaskRows(sorted, Quadrant.NEITHER)
    }

    /**
     * 排序模式 -> 比较器。
     *
     * - [SortMode.PRIORITY]：轻重缓急（优先级 -> 创建时间正序）
     * - [SortMode.DUE_DATE]：截止日期升序，无截止日期排最后，同时按创建时间升序
     * - [SortMode.CREATED_AT]：创建时间升序，同时按 id 升序
     */
    private fun comparatorFor(prefs: AppPrefs): Comparator<TaskEntity> = when (prefs.sortMode) {
        SortMode.PRIORITY -> compareBy<TaskEntity> { priorityRank(it) }
            .thenBy { it.createdAt }
        SortMode.DUE_DATE -> dueDateComparator()
        SortMode.CREATED_AT -> createdAtComparator()
    }

    /** 生成任务行（已无 assignee 分隔逻辑） */
    private fun buildTaskRows(
        tasks: List<TaskEntity>,
        quadrant: Quadrant
    ): List<BoardRow> = tasks.map { BoardRow.TaskRowItem(it, quadrant) }

    // ------------------------------------------------------------ 交互

    fun setQuery(value: String) {
        query.value = value.take(120)
    }

    fun openSearch() {
        searchExpanded.value = true
    }

    fun closeSearch() {
        searchExpanded.value = false
        query.value = ""
    }

    fun onSearchStateReset() {
        query.value = ""
        searchExpanded.value = false
    }

    fun toggleFolded(quadrant: Quadrant) {
        val current = uiState.value.foldedGroups[quadrant.groupKey] == true
        viewModelScope.launch {
            preferences.setGroupFolded(quadrant.groupKey, !current)
        }
    }

    /** 眼睛图标：全部展开 / 全部收起（同步 DataStore 的 foldedGroups） */
    fun toggleAllFolded() {
        val fold = !uiState.value.allGroupsFolded
        viewModelScope.launch {
            preferences.setAllGroupsFolded(Quadrant.ordered.map { it.groupKey }, fold)
        }
    }

    /**
     * 写入本次进入主屏的筛选条件（唯一来源是路由参数 `board?filter=`）。
     *
     * 不回写任何持久化存储：路由参数自己就是持久化的那一份。
     */
    fun setRouteFilter(filter: BoardFilter) {
        routeFilter.value = filter
    }

    /** 切换排序模式（折叠状态仅轻重缓急模式有意义，切走时保留在 DataStore） */
    fun setSortMode(mode: SortMode) {
        viewModelScope.launch { preferences.setSortMode(mode) }
    }

    /** 切换已完成显示方式 */
    fun setCompletedStyle(style: CompletedStyle) {
        viewModelScope.launch { preferences.setCompletedStyle(style) }
    }

    /** 三态主题：跟随系统 / 深色 / 浅色 */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    /** 删除任务图片文件夹（延迟删除窗口结束后调用） */
    fun deleteTaskImages(taskId: Long) {
        viewModelScope.launch { repository.deleteTaskImages(taskId) }
    }

    /** HIGHLIGHT：任务在隐藏状态时临时显示，离开主屏复位 */
    fun setTransientVisible(taskId: Long?) {
        transientVisible.value = if (taskId == null) emptySet() else setOf(taskId)
    }

    fun clearTransientVisible() {
        transientVisible.value = emptySet()
    }

    fun toggleCheckedOff(task: TaskEntity) {
        viewModelScope.launch { repository.setCheckedOff(task.id, !task.isCheckedOff) }
    }

    fun setImportant(task: TaskEntity, value: Boolean) {
        viewModelScope.launch { repository.setImportant(task.id, value) }
    }

    fun setUrgent(task: TaskEntity, value: Boolean) {
        viewModelScope.launch { repository.setUrgent(task.id, value) }
    }

    /** 立即删除（重要任务确认后 / 归档永久删除） */
    fun deleteNow(task: TaskEntity) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    /** 撤销完成状态 */
    fun uncomplete(task: TaskEntity) {
        viewModelScope.launch { repository.setCheckedOff(task.id, false) }
    }

    class Factory(
        private val repository: TaskRepository,
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BoardViewModel(
                repository = repository,
                preferences = preferences
            ) as T
    }
}
