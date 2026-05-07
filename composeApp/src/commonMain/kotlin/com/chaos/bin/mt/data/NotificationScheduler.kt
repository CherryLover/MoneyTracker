package com.chaos.bin.mt.data

expect class NotificationScheduler {
    suspend fun rescheduleAll(schedules: List<ReminderSchedule>)
    suspend fun cancelAll()
}
