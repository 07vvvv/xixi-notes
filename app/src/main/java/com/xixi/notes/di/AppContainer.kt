package com.xixi.notes.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import com.xixi.notes.data.local.TaskDatabase
import com.xixi.notes.data.preferences.AppPreferences
import com.xixi.notes.data.preferences.FIRST_LAUNCH_DONE
import com.xixi.notes.data.preferences.dataStore
import com.xixi.notes.data.repository.TaskRepository
import com.xixi.notes.image.ImageManager
import com.xixi.notes.image.ImageViewerBridge
import com.xixi.notes.reminder.TaskReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * 导航事件：由通知点击 / 深链传递。
 *
 * @param taskId 目标任务 id
 * @param action [com.xixi.notes.reminder.NotificationAction] 的名称
 */
data class NavEvent(val taskId: Long, val action: String)

/** Radial Menu 状态：含 FAB 中心坐标 */
data class RadialMenuState(val centerX: Float, val centerY: Float)

/**
 * 手动依赖注入容器。
 *
 * 单例，由 [com.xixi.notes.XixiNotesApp] 创建，通过 [LocalAppContainer] 下发。
 */
class AppContainer(private val context: Context) {

    // Room 数据库
    private val database: TaskDatabase = Room.databaseBuilder(
        context,
        TaskDatabase::class.java,
        "xixi_notes.db"
    ).build()

    // 偏好
    private val dataStore = context.dataStore

    // 图片管理（先于 Repository 创建，二者共用同一实例）
    val imageManager = ImageManager(context)

    // Repository
    val taskRepository = TaskRepository(database.taskDao(), imageManager)

    // 图片查看桥
    val imageViewerBridge = ImageViewerBridge()

    // 提醒调度
    val reminderScheduler = TaskReminderScheduler(context)

    // 偏好读写
    val preferences = AppPreferences(dataStore)

    // Radial Menu 状态
    val radialMenuState = MutableStateFlow<RadialMenuState?>(null)

    // 启动就绪标志
    val startupReady = MutableStateFlow(false)

    // 通知导航事件（Channel 缓冲，一次性事件）
    private val _navEvents = Channel<NavEvent>(Channel.BUFFERED)
    val navEvents: Flow<NavEvent> = _navEvents.receiveAsFlow()

    suspend fun sendNavEvent(event: NavEvent) {
        _navEvents.send(event)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // 异步初始化：读 DataStore → 首次启动清理临时文件 → 置 startupReady
        scope.launch {
            try {
                val prefs = dataStore.data.first()
                val firstLaunchDone = prefs[FIRST_LAUNCH_DONE] ?: false
                if (!firstLaunchDone) {
                    imageManager.cleanTempDir()
                    dataStore.edit { it[FIRST_LAUNCH_DONE] = true }
                }
            } catch (e: Exception) {
                // 初始化失败也要放行启动，避免白屏卡死
            } finally {
                startupReady.value = true
            }
        }
    }
}

/** 全局 AppContainer 下发通道 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
