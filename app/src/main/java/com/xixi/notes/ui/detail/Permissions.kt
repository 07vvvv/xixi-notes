package com.xixi.notes.ui.detail

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

/**
 * 提醒相关权限。
 *
 * 点击提醒时间选择器时一并处理：
 * 1. POST_NOTIFICATIONS（Android 13+ 运行时权限）
 * 2. SCHEDULE_EXACT_ALARM（Android 12+ 需要用户在系统设置里授权）
 *
 * 两者被拒绝时分别提示；提醒仍然创建（精确闹钟不可用时降级为非精确）。
 */
class ReminderPermissionState internal constructor(
    private val context: Context,
    val notificationMessage: MutableState<String?>,
    val exactAlarmMessage: MutableState<String?>,
    private val requestNotification: () -> Unit,
    private val onExactAlarmCheck: () -> Unit
) {
    val hasNotificationPermission: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    val canScheduleExactAlarms: Boolean
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
            val manager = context.getSystemService(AlarmManager::class.java) ?: return true
            return manager.canScheduleExactAlarms()
        }

    /** 依次请求两个权限；被拒后分别提示 */
    fun request() {
        if (!hasNotificationPermission) {
            requestNotification()
        } else {
            onExactAlarmCheck()
        }
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

    fun dismissMessages() {
        notificationMessage.value = null
        exactAlarmMessage.value = null
    }
}

/** 记住提醒相关权限状态并提供请求入口 */
@Composable
fun rememberReminderPermissions(context: Context): ReminderPermissionState {
    val notificationMessage = remember { mutableStateOf<String?>(null) }
    val exactAlarmMessage = remember { mutableStateOf<String?>(null) }
    var pendingExactAlarm by remember { mutableStateOf(false) }
    var stateHolder by remember { mutableStateOf<ReminderPermissionState?>(null) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            notificationMessage.value = "未授予通知权限，任务到期将不会提醒"
        }
        // 通知权限处理完后继续处理精确闹钟权限
        pendingExactAlarm = true
    }

    val state = remember(context) {
        ReminderPermissionState(
            context = context,
            notificationMessage = notificationMessage,
            exactAlarmMessage = exactAlarmMessage,
            requestNotification = {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onExactAlarmCheck = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(AlarmManager::class.java)
                    if (manager != null && !manager.canScheduleExactAlarms()) {
                        exactAlarmMessage.value = "未授予精确闹钟权限，提醒可能延后数分钟"
                    }
                }
            }
        )
    }
    stateHolder = state

    LaunchedEffect(pendingExactAlarm) {
        if (pendingExactAlarm) {
            pendingExactAlarm = false
            stateHolder?.request()
        }
    }

    return state
}

/**
 * 相机可用性。
 *
 * 无相机硬件或 CAMERA 权限被拒绝时，编辑页隐藏「拍照」入口。
 */
@Composable
fun rememberCameraAvailability(
    context: Context,
    requestGranted: Boolean,
    onRequestGranted: () -> Unit
): CameraAvailability {
    val hasHardware = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var requested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        granted = result
        if (result) onRequestGranted()
    }

    // 外部请求一次权限（点击「拍照」但未授权）
    LaunchedEffect(requestGranted) {
        if (requestGranted && !granted && hasHardware && !requested) {
            requested = true
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    return remember(hasHardware, granted) {
        CameraAvailability(
            hasHardware = hasHardware,
            granted = granted,
            launchPermission = { launcher.launch(Manifest.permission.CAMERA) }
        )
    }
}

/** 相机可用性快照 */
data class CameraAvailability(
    val hasHardware: Boolean,
    val granted: Boolean,
    val launchPermission: () -> Unit
) {
    /** 是否展示「拍照」：需要硬件与授权同时满足 */
    val canTakePhoto: Boolean get() = hasHardware && granted

    fun requestPermission() = launchPermission()
}
