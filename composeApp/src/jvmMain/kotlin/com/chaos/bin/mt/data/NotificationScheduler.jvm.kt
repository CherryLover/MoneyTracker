package com.chaos.bin.mt.data

actual class NotificationScheduler {
    actual suspend fun rescheduleAll(schedules: List<ReminderSchedule>) = Unit
    actual suspend fun cancelAll() = Unit
}
