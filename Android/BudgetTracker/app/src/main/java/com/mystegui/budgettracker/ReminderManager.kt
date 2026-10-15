package com.mystegui.budgettracker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderManager {

    const val CHANNEL_ID = "budget_reminder_channel"
    const val NOTIFICATION_ID = 1001
    const val PREFS_KEY_HOUR = "reminder_hour"
    const val PREFS_KEY_MINUTE = "reminder_minute"
    const val DEFAULT_HOUR = 20    // 8 PM
    const val DEFAULT_MINUTE = 0

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Expense Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to log your daily expenses"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(context: Context, hour: Int, minute: Int) {
        // Save the time to prefs
        context.getSharedPreferences("budget_tracker", Context.MODE_PRIVATE)
            .edit()
            .putInt(PREFS_KEY_HOUR, hour)
            .putInt(PREFS_KEY_MINUTE, minute)
            .apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set the time for today, or tomorrow if the time has already passed
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Schedule repeating daily alarm
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun getSavedHour(context: Context): Int =
        context.getSharedPreferences("budget_tracker", Context.MODE_PRIVATE)
            .getInt(PREFS_KEY_HOUR, DEFAULT_HOUR)

    fun getSavedMinute(context: Context): Int =
        context.getSharedPreferences("budget_tracker", Context.MODE_PRIVATE)
            .getInt(PREFS_KEY_MINUTE, DEFAULT_MINUTE)

    fun isReminderEnabled(context: Context): Boolean =
        context.getSharedPreferences("budget_tracker", Context.MODE_PRIVATE)
            .getBoolean("reminder_enabled", false)

    fun setReminderEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("budget_tracker", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("reminder_enabled", enabled)
            .apply()
    }
}