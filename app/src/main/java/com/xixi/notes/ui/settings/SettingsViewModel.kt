package com.xixi.notes.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xixi.notes.data.preferences.AppPrefs
import com.xixi.notes.data.preferences.AppPreferences
import com.xixi.notes.data.preferences.NotificationActionMode
import com.xixi.notes.ui.board.CompletedStyle
import com.xixi.notes.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 提醒提前分钟数可选项 */
val ReminderMinuteOptions = listOf(0, 5, 10, 15, 30, 60)

class SettingsViewModel(private val preferences: AppPreferences) : ViewModel() {

    val prefs: StateFlow<AppPrefs> = preferences.prefs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppPrefs()
    )

    fun setCompletedStyle(style: CompletedStyle) {
        viewModelScope.launch { preferences.setCompletedStyle(style) }
    }

    fun setDefaultReminderMinutes(minutes: Int) {
        viewModelScope.launch { preferences.setDefaultReminderMinutes(minutes) }
    }

    fun setNotificationAction(mode: NotificationActionMode) {
        viewModelScope.launch { preferences.setNotificationAction(mode) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    class Factory(private val preferences: AppPreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(preferences) as T
    }
}
