package com.mystegui.budgettracker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        // Re-schedule on boot
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (ReminderManager.isReminderEnabled(context)) {
                ReminderManager.scheduleReminder(
                    context,
                    ReminderManager.getSavedHour(context),
                    ReminderManager.getSavedMinute(context)
                )
            }
            return
        }

        // Build the notification
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val messages = listOf(
            "Don't forget to log your expenses today!",
            "Hey! Have you tracked your spending today?",
            "Quick reminder — log your expenses before you forget!",
            "Your budget tracker misses you. Log today's expenses!",
            "Stay on top of your budget — log your expenses now!"
        )
        val message = messages.random()

        val notification = NotificationCompat.Builder(context, ReminderManager.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Student Budget Tracker")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(ReminderManager.NOTIFICATION_ID, notification)
    }
}