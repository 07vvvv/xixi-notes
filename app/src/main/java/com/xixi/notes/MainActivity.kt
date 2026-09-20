package com.xixi.notes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.xixi.notes.data.preferences.AppPrefs
import com.xixi.notes.di.AppContainer
import com.xixi.notes.di.LocalAppContainer
import com.xixi.notes.di.NavEvent
import com.xixi.notes.reminder.NotificationAction
import com.xixi.notes.ui.main.MainScaffold
import com.xixi.notes.ui.theme.XixiNotesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 顺序要求：installSplashScreen 必须在 super.onCreate 之前
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // 顺序要求：enableEdgeToEdge 必须在 setContent 之前
        enableEdgeToEdge()

        val app = application as XixiNotesApp
        // 直接用 value，不用 collectAsStateWithLifecycle（Splash 条件是一帧内同步读取）
        splash.setKeepOnScreenCondition { !app.container.startupReady.value }

        // 冷启动时通知可能带着数据进入（此时不会走 onNewIntent）
        handleIntent(intent)

        setContent {
            AppRoot(container = app.container)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** 通知 / 深链传来的 taskId + action 转成容器里的一次性事件 */
    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val taskId = intent.getLongExtra("taskId", -1L)
        if (taskId <= 0L) return
        val action = intent.getStringExtra("action") ?: NotificationAction.OPEN_EDIT.name
        val container = (application as XixiNotesApp).container
        // 生命周期作用域，不用 GlobalScope
        lifecycleScope.launch {
            container.sendNavEvent(NavEvent(taskId, action))
        }
    }
}

/** 主题与容器的根组合 */
@Composable
private fun AppRoot(container: AppContainer) {
    val prefs by container.preferences.prefs.collectAsState(initial = AppPrefs())
    XixiNotesTheme(mode = prefs.themeMode, dynamicColor = prefs.dynamicColor) {
        CompositionLocalProvider(LocalAppContainer provides container) {
            MainScaffold()
        }
    }
}
