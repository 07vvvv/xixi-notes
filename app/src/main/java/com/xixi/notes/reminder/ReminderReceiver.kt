package com.xixi.notes.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.xixi.notes.MainActivity
import com.xixi.notes.R
import com.xixi.notes.XixiNotesApp
import com.xixi.notes.data.local.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * 提醒广播接收器。
 *
 * 用 goAsync() + Dispatchers.IO 在后台读数据库并发送通知，
 * 不阻塞广播主线程。
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_ID = "taskId"
        const val CHANNEL_ID = "xixi_task_reminder"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 复用进程内的 AppContainer，避免为同一个数据库再开一个 Room 实例
                val container = (appContext as? XixiNotesApp)?.container
                val task = container?.taskRepository?.getTaskById(taskId)
                if (task == null) {
                    // 任务已删除：移除残留通知
                    NotificationManagerCompat.from(appContext).cancel(taskId.toInt())
                    return@launch
                }
                sendNotification(appContext, task)
            } catch (e: Exception) {
                // 提醒失败不应崩溃
            } finally {
                pending.finish()
            }
        }
    }

    private fun sendNotification(context: Context, task: TaskEntity) {
        ensureChannel(context)

        // Android 13+ 无通知权限时直接跳过
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("taskId", task.id)
            putExtra("action", NotificationAction.OPEN_EDIT.name)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val subtitle = task.dueDate?.let {
            DateFormat.getDateTimeInstance().format(Date(it))
        } ?: task.reminderTime?.let {
            DateFormat.getDateTimeInstance().format(Date(it))
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_content, task.title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        if (subtitle != null) {
            builder.setSubText(subtitle)
        }

        try {
            NotificationManagerCompat.from(context).notify(task.id.toInt(), builder.build())
        } catch (e: SecurityException) {
            // 权限被回收，忽略
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}
