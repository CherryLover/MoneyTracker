package com.chaos.bin.mt.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

const val ReminderChannelId = "reminder"

fun ensureReminderNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
        ReminderChannelId,
        "记账提醒",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "每日定时提醒记账"
    }
    manager.createNotificationChannel(channel)
}
