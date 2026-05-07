package com.chaos.bin.mt.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chaos.bin.mt.data.NotificationScheduler
import com.chaos.bin.mt.data.PreferenceRepository
import com.chaos.bin.mt.data.ReminderRepository
import com.chaos.bin.mt.db.DatabaseFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val db = DatabaseFactory(appContext).create()
                val schedules = ReminderRepository(PreferenceRepository(db)).list()
                NotificationScheduler(appContext).rescheduleAll(schedules)
            } finally {
                pending.finish()
            }
        }
    }
}
