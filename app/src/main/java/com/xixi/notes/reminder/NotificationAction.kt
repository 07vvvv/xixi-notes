package com.xixi.notes.reminder

import androidx.annotation.StringRes
import com.xixi.notes.R
import com.xixi.notes.data.preferences.NotificationActionMode

/** 通知点击行为（传给 MainActivity 的 action 字符串） */
enum class NotificationAction(@StringRes val labelRes: Int) {
    OPEN_EDIT(R.string.settings_notification_open_edit),
    OPEN_HOME(R.string.settings_notification_open_home),
    HIGHLIGHT(R.string.settings_notification_highlight);

    companion object {
        /** 从意图字符串解析，未知值回落到 OPEN_EDIT */
        fun fromString(raw: String?): NotificationAction = when (raw) {
            OPEN_HOME.name -> OPEN_HOME
            HIGHLIGHT.name -> HIGHLIGHT
            else -> OPEN_EDIT
        }

        fun fromMode(mode: NotificationActionMode): NotificationAction = when (mode) {
            NotificationActionMode.OPEN_EDIT -> OPEN_EDIT
            NotificationActionMode.OPEN_HOME -> OPEN_HOME
            NotificationActionMode.HIGHLIGHT -> HIGHLIGHT
        }
    }
}
