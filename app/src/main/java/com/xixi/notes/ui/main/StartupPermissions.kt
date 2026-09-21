package com.xixi.notes.ui.main

import android.Manifest
import android.app.AlarmManager
import android.content.ActivityNotFoundException
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

/** 逾期筛选的标记值 */
const val OVERDUE_FILTER = "OVERDUE"

/**
 * 跳转到系统通知设置。
 * 优先 `ACTION_APP_NOTIFICATION_SETTINGS`，失败回退到应用详情页。
 */
fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        openAppDetailsSettings(context)
    }
}

/** 跳转到应用详情页（兜底） */
fun openAppDetailsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

/** 跳转到精确闹钟授权页 */
fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

/** 是否已授予通知权限（Android 13 以下视为已授予） */
fun hasNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

/** 是否可调度精确闹钟（Android 12 以下视为可用） */
fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val manager = context.getSystemService(AlarmManager::class.java) ?: return true
    return manager.canScheduleExactAlarms()
}

/**
 * 首次启动的权限申请控制器。
 *
 * 顺序：先 `POST_NOTIFICATIONS`（Android 13+ 运行时权限），
 * 通知处理完后再检查 `SCHEDULE_EXACT_ALARM`（Android 12+ 需跳系统设置授权）。
 * 被拒绝不阻断流程，设置页会展示状态并提供「去开启」。
 */
class StartupPermissionController internal constructor(
    private val requestNotification: () -> Unit,
    private val checkExactAlarm: () -> Unit
) {
    /** 依次申请 */
    fun requestAll() {
        requestNotification()
    }

    /** 通知权限处理完毕，继续检查精确闹钟 */
    internal fun continueWithExactAlarm() = checkExactAlarm()
}

@Composable
fun rememberStartupPermissions(context: Context): StartupPermissionController {
    var pendingExactAlarm by remember { mutableStateOf(false) }
    val latestContext by rememberUpdatedState(context)

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // 无论是否授权，都继续处理精确闹钟
        pendingExactAlarm = true
    }

    val controller = remember(context) {
        StartupPermissionController(
            requestNotification = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !hasNotificationPermission(context)
                ) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // 无需申请通知权限，直接处理精确闹钟
                    pendingExactAlarm = true
                }
            },
            checkExactAlarm = {
                if (!canScheduleExactAlarms(latestContext)) {
                    openExactAlarmSettings(latestContext)
                }
            }
        )
    }

    // 通知权限流程结束后接着处理精确闹钟
    LaunchedEffect(pendingExactAlarm) {
        if (pendingExactAlarm) {
            pendingExactAlarm = false
            controller.continueWithExactAlarm()
        }
    }

    return controller
}
