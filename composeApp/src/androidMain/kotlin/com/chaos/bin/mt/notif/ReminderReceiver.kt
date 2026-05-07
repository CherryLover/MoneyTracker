package com.chaos.bin.mt.notif

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chaos.bin.mt.MainActivity
import com.chaos.bin.mt.R
import com.chaos.bin.mt.data.NotificationPermission
import com.chaos.bin.mt.data.NotificationScheduler
import com.chaos.bin.mt.data.PreferenceRepository
import com.chaos.bin.mt.data.ReminderMessages
import com.chaos.bin.mt.data.ReminderRepository
import com.chaos.bin.mt.db.DatabaseFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (scheduleId <= 0L) return@launch
                val db = DatabaseFactory(appContext).create()
                val reminderRepository = ReminderRepository(PreferenceRepository(db))
                val schedule = reminderRepository.list()
                    .firstOrNull { it.id == scheduleId && it.enabled }
                    ?: return@launch
                val permission = NotificationPermission(appContext)
                if (permission.isGranted()) {
                    showReminderNotification(appContext, scheduleId)
                }
                NotificationScheduler(appContext).scheduleOne(schedule)
            } finally {
                pending.finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showReminderNotification(context: Context, scheduleId: Long) {
        ensureReminderNotificationChannel(context)
        val openIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            openIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, ReminderChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ReminderMessages.title)
            .setContentText(ReminderMessages.pickBody())
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(scheduleId.hashCode(), notification)
    }

    companion object {
        const val EXTRA_SCHEDULE_ID = "scheduleId"
    }
}
