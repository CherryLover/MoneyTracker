package com.chaos.bin.mt.data

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

actual class NotificationScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    actual suspend fun rescheduleAll(schedules: List<ReminderSchedule>) {
        cancelAll()
        schedules.filter { it.enabled }.forEach { schedule ->
            val content = UNMutableNotificationContent().apply {
                setTitle(ReminderMessages.title)
                setBody(ReminderMessages.pickBody())
                setSound(UNNotificationSound.defaultSound())
            }
            val components = NSDateComponents().apply {
                hour = schedule.hour.toLong()
                minute = schedule.minute.toLong()
            }
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                dateComponents = components,
                repeats = true,
            )
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = "reminder_${schedule.id}",
                content = content,
                trigger = trigger,
            )
            center.addNotificationRequest(request) { error ->
                if (error != null) println("[NotificationScheduler] add failed: $error")
            }
        }
    }

    actual suspend fun cancelAll() {
        val ids = suspendCoroutine<List<String>> { cont ->
            center.getPendingNotificationRequestsWithCompletionHandler { requests ->
                val reminderIds = requests.orEmpty()
                    .filterIsInstance<UNNotificationRequest>()
                    .map { it.identifier }
                    .filter { it.startsWith("reminder_") }
                cont.resume(reminderIds)
            }
        }
        if (ids.isNotEmpty()) {
            center.removePendingNotificationRequestsWithIdentifiers(ids)
        }
    }
}
