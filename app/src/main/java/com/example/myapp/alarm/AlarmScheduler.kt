package com.example.myapp.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.myapp.data.Task

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTaskAlarm(task: Task) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_START_ALARM
            putExtra(AlarmReceiver.EXTRA_TASK_ID, task.id)
            putExtra(AlarmReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(AlarmReceiver.EXTRA_AUTO_QUIT_MINUTES, task.autoQuitDurationMinutes)
            putExtra(AlarmReceiver.EXTRA_RINGTONE_URI, task.ringtoneUri)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Only schedule if time is in the future
        if (task.timestampMs > System.currentTimeMillis()) {
            val showIntent = Intent(context, com.example.myapp.MainActivity::class.java)
            val showPendingIntent = PendingIntent.getActivity(
                context,
                task.id,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmClockInfo = AlarmManager.AlarmClockInfo(task.timestampMs, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        }
    }

    fun cancelTaskAlarm(taskId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
