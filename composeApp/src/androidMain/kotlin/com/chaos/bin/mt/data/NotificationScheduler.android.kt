package com.chaos.bin.mt.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import com.chaos.bin.mt.notif.ReminderReceiver

actual class NotificationScheduler(private val context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = appContext.getSharedPreferences(SchedulerPrefsName, Context.MODE_PRIVATE)

    actual suspend fun rescheduleAll(schedules: List<ReminderSchedule>) {
        cancelAll()
        val enabled = schedules.filter { it.enabled }
        enabled.forEach { scheduleOne(it) }
        prefs.edit {
            putStringSet(ScheduledIdsKey, enabled.map { it.id.toString() }.toSet())
        }
    }

    actual suspend fun cancelAll() {
        val ids = prefs.getStringSet(ScheduledIdsKey, emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }
        ids.forEach { id ->
            existingPendingIntent(id)?.let { pi ->
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
        prefs.edit { remove(ScheduledIdsKey) }
    }

    internal fun scheduleOne(schedule: ReminderSchedule, nowMillis: Long = nowMillis()) {
        if (!schedule.enabled) return
        val triggerAt = computeNextTriggerMillis(schedule.hour, schedule.minute, nowMillis)
        val pi = pendingIntent(schedule.id, PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun existingPendingIntent(id: Long): PendingIntent? =
        PendingIntent.getBroadcast(
            appContext,
            id.hashCode(),
            reminderIntent(id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )

    private fun pendingIntent(id: Long, extraFlags: Int): PendingIntent {
        return PendingIntent.getBroadcast(
            appContext,
            id.hashCode(),
            reminderIntent(id),
            PendingIntent.FLAG_IMMUTABLE or extraFlags,
        )
    }

    private fun reminderIntent(id: Long): Intent =
        Intent(appContext, ReminderReceiver::class.java)
            .putExtra(ReminderReceiver.EXTRA_SCHEDULE_ID, id)
}

private const val SchedulerPrefsName = "moneytracker_reminder_scheduler"
private const val ScheduledIdsKey = "scheduled_ids"
