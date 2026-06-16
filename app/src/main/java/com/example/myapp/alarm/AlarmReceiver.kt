package com.example.myapp.alarm

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.myapp.MainActivity
import com.example.myapp.R

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_START_ALARM = "com.example.myapp.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.myapp.ACTION_STOP_ALARM"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_TITLE = "EXTRA_TASK_TITLE"
        const val EXTRA_AUTO_QUIT_MINUTES = "EXTRA_AUTO_QUIT_MINUTES"
        const val EXTRA_RINGTONE_URI = "EXTRA_RINGTONE_URI"
        const val CHANNEL_ID = "task_alarms"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)

        when (intent.action) {
            ACTION_START_ALARM -> {
                if (taskId == -1) return
                val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Alarm"
                val autoQuitMinutes = intent.getIntExtra(EXTRA_AUTO_QUIT_MINUTES, 5)
                val ringtoneUriStr = intent.getStringExtra(EXTRA_RINGTONE_URI)

                val ringtoneUri = if (!ringtoneUriStr.isNullOrEmpty()) {
                    android.net.Uri.parse(ringtoneUriStr)
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                }

                val channelId = if (!ringtoneUriStr.isNullOrEmpty()) {
                    "task_alarms_${ringtoneUriStr.hashCode()}"
                } else {
                    CHANNEL_ID
                }

                createNotificationChannel(notificationManager, channelId, ringtoneUri)

                // Dismiss intent
                val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_STOP_ALARM
                    putExtra(EXTRA_TASK_ID, taskId)
                }
                val dismissPendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskId,
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Main activity intent
                val contentIntent = Intent(context, MainActivity::class.java)
                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    taskId,
                    contentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle(title)
                    .setContentText("It's time for your task!")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
                    .setOngoing(true) // Cannot be swiped away easily
                    .build()

                notification.flags = notification.flags or Notification.FLAG_INSISTENT // Loop sound

                notificationManager.notify(taskId, notification)

                // Schedule auto-quit
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val autoQuitTime = System.currentTimeMillis() + (autoQuitMinutes * 60 * 1000L)
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    autoQuitTime,
                    dismissPendingIntent
                )
            }
            ACTION_STOP_ALARM -> {
                if (taskId != -1) {
                    notificationManager.cancel(taskId)
                }
            }
        }
    }

    private fun createNotificationChannel(notificationManager: NotificationManager, channelId: String, ringtoneUri: android.net.Uri) {
        val channelName = if (channelId == CHANNEL_ID) "Task Alarms" else "Task Alarm Ringtone Channel"
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for task alarms"
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            setSound(ringtoneUri, audioAttributes)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
