package com.xixi.notes.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xixi.notes.ui.board.AssigneeMode
import com.xixi.notes.ui.board.CompletedStyle
import com.xixi.notes.ui.board.SortMode
import com.xixi.notes.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 通知点击行为 */
enum class NotificationActionMode {
    /** 打开编辑页（默认） */
    OPEN_EDIT,

    /** 只打开主屏 */
    OPEN_HOME,

    /** 打开主屏并高亮该任务 */
    HIGHLIGHT
}

/** 折叠状态键：JSON Map<String, Boolean> */
val FOLDED_GROUPS = stringPreferencesKey("folded_groups")
val FIRST_LAUNCH_DONE = booleanPreferencesKey("first_launch_done")

/** 引导页是否已展示（仅首次启动展示一次） */
val ONBOARDING_SHOWN = booleanPreferencesKey("onboarding_shown")

/** 首次启动的权限申请是否已发起（通知 + 精确闹钟只申请一次） */
val PERMISSIONS_REQUESTED = booleanPreferencesKey("permissions_requested")
val COMPLETED_STYLE = stringPreferencesKey("completed_style")
val ASSIGNEE_MODE = stringPreferencesKey("assignee_mode")
val SORT_MODE = stringPreferencesKey("sort_mode")
val DEFAULT_REMINDER_MINUTES = intPreferencesKey("default_reminder_minutes")
val NOTIFICATION_ACTION = stringPreferencesKey("notification_action")
val THEME_MODE = stringPreferencesKey("theme_mode")

/** DataStore 实例（进程内单例） */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "xixi_notes_prefs")

/** 应用偏好快照 */
data class AppPrefs(
    val completedStyle: CompletedStyle = CompletedStyle.IN_PLACE,
    val assigneeMode: AssigneeMode = AssigneeMode.SEPARATED,
    val sortMode: SortMode = SortMode.PRIORITY,
    val defaultReminderMinutes: Int = 10,
    val notificationAction: NotificationActionMode = NotificationActionMode.OPEN_EDIT,
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    /** 引导页是否已展示 */
    val onboardingShown: Boolean = false,
    /** 首次启动权限申请是否已发起 */
    val permissionsRequested: Boolean = false,
    /** 四分组折叠状态（仅轻重缓急模式有意义，跨进程保留） */
    val foldedGroups: Map<String, Boolean> = emptyMap()
)

/** 偏好读写：全部基于 Preferences DataStore */
class AppPreferences(private val dataStore: DataStore<Preferences>) {

    val prefs: Flow<AppPrefs> = dataStore.data.map { p ->
        AppPrefs(
            completedStyle = p[COMPLETED_STYLE].toEnum(CompletedStyle.IN_PLACE),
            assigneeMode = p[ASSIGNEE_MODE].toEnum(AssigneeMode.SEPARATED),
            sortMode = p[SORT_MODE].toEnum(SortMode.PRIORITY),
            defaultReminderMinutes = p[DEFAULT_REMINDER_MINUTES] ?: 10,
            notificationAction = p[NOTIFICATION_ACTION].toEnum(NotificationActionMode.OPEN_EDIT),
            themeMode = p[THEME_MODE].toEnum(ThemeMode.FOLLOW_SYSTEM),
            onboardingShown = p[ONBOARDING_SHOWN] ?: false,
            permissionsRequested = p[PERMISSIONS_REQUESTED] ?: false,
            foldedGroups = decodeFolded(p[FOLDED_GROUPS])
        )
    }

    /** 读取引导页是否已展示（启动时决定起始路由） */
    suspend fun isOnboardingShown(): Boolean =
        dataStore.data.map { it[ONBOARDING_SHOWN] ?: false }.first()

    /** 标记引导页已展示（写完再导航，避免每次启动重复弹出） */
    suspend fun setOnboardingShown(shown: Boolean) {
        dataStore.edit { it[ONBOARDING_SHOWN] = shown }
    }

    /** 读取首次权限申请是否已发起 */
    suspend fun isPermissionsRequested(): Boolean =
        dataStore.data.map { it[PERMISSIONS_REQUESTED] ?: false }.first()

    /** 标记首次权限申请已发起 */
    suspend fun setPermissionsRequested(requested: Boolean) {
        dataStore.edit { it[PERMISSIONS_REQUESTED] = requested }
    }

    suspend fun setSortMode(mode: SortMode) {
        dataStore.edit { it[SORT_MODE] = mode.name }
    }

    suspend fun setCompletedStyle(style: CompletedStyle) {
        dataStore.edit { it[COMPLETED_STYLE] = style.name }
    }

    suspend fun setAssigneeMode(mode: AssigneeMode) {
        dataStore.edit { it[ASSIGNEE_MODE] = mode.name }
    }

    suspend fun setDefaultReminderMinutes(minutes: Int) {
        dataStore.edit { it[DEFAULT_REMINDER_MINUTES] = minutes }
    }

    suspend fun setNotificationAction(mode: NotificationActionMode) {
        dataStore.edit { it[NOTIFICATION_ACTION] = mode.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    /** 一次性写入四个分组的折叠状态（展开/收起全部） */
    suspend fun setAllGroupsFolded(groupKeys: List<String>, folded: Boolean) {
        dataStore.edit { p ->
            val map = decodeFolded(p[FOLDED_GROUPS]).toMutableMap()
            groupKeys.forEach { key -> map[key] = folded }
            p[FOLDED_GROUPS] = Json.encodeToString(map)
        }
    }

    /** 更新某个分组的折叠状态 */
    suspend fun setGroupFolded(groupKey: String, folded: Boolean) {
        dataStore.edit { p ->
            val map = decodeFolded(p[FOLDED_GROUPS]).toMutableMap()
            map[groupKey] = folded
            p[FOLDED_GROUPS] = Json.encodeToString(map)
        }
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(fallback: T): T {
    if (this == null) return fallback
    return try {
        enumValueOf<T>(this)
    } catch (e: IllegalArgumentException) {
        fallback
    }
}

/** 容错解析折叠状态 JSON */
fun decodeFolded(raw: String?): Map<String, Boolean> {
    if (raw.isNullOrBlank()) return emptyMap()
    return try {
        Json.decodeFromString<Map<String, Boolean>>(raw)
    } catch (e: Exception) {
        emptyMap()
    }
}
