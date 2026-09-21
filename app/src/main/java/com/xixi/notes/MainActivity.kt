package com.xixi.notes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.xixi.notes.di.AppContainer
import com.xixi.notes.di.LocalAppContainer
import com.xixi.notes.di.NavEvent
import com.xixi.notes.reminder.NotificationAction
import com.xixi.notes.ui.main.MainScaffold
import com.xixi.notes.ui.theme.ThemeMode
import com.xixi.notes.ui.theme.XixiNotesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "XixiNotes"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 顺序要求：installSplashScreen 必须在 super.onCreate 之前
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // 顺序要求：enableEdgeToEdge 必须在 setContent 之前
        enableEdgeToEdge()

        // 兜底：记录未捕获异常，便于排查「不定时黑屏」这类偶发问题
        installCrashLogger()

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

    /** 记录未捕获异常到 logcat（保留默认崩溃行为，只是多打一条带上下文的日志） */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "未捕获异常 thread=${thread.name}", throwable)
            previous?.uncaughtException(thread, throwable)
        }
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

/**
 * 主题与容器的根组合。
 *
 * 只订阅主题模式与引导状态，避免无关偏好变化触发整棵树重组。
 */
@Composable
private fun AppRoot(container: AppContainer) {
    // 主题模式（首帧跟随系统，Splash 期间完成真实值读取）
    val themeMode: State<ThemeMode> = produceState(
        initialValue = ThemeMode.FOLLOW_SYSTEM,
        key1 = container
    ) {
        container.preferences.prefs.collect { prefs -> value = prefs.themeMode }
    }

    // 引导是否已展示（决定起始路由）。首帧为 false，与 Splash 的 startupReady 同步完成
    val onboardingShown: State<Boolean> = produceState(
        initialValue = false,
        key1 = container
    ) {
        container.preferences.prefs.collect { prefs -> value = prefs.onboardingShown }
    }

    XixiNotesTheme(mode = themeMode.value) {
        CompositionLocalProvider(LocalAppContainer provides container) {
            MainScaffold(onboardingShown = onboardingShown.value)
        }
    }
}
