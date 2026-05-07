package com.chaos.bin.mt.data

import kotlinx.serialization.Serializable

@Serializable
data class ReminderSchedule(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
)

object ReminderRules {
    const val MAX_COUNT = 3
    const val MIN_GAP_MINUTES = 60

    fun findConflict(
        target: Pair<Int, Int>,
        existing: List<ReminderSchedule>,
        excludingId: Long? = null,
    ): ReminderSchedule? {
        val targetMinutes = minutesOfDay(target.first, target.second)
        return existing
            .filter { it.id != excludingId }
            .firstOrNull { other ->
                val otherMinutes = minutesOfDay(other.hour, other.minute)
                kotlin.math.abs(targetMinutes - otherMinutes) < MIN_GAP_MINUTES
            }
    }

    fun nextId(existing: List<ReminderSchedule>): Long =
        ((existing.maxOfOrNull { it.id } ?: 0L) + 1L).coerceAtLeast(1L)

    fun isValidTime(hour: Int, minute: Int): Boolean = hour in 0..23 && minute in 0..59
}

fun minutesOfDay(hour: Int, minute: Int): Int = hour * 60 + minute

fun formatReminderTime(hour: Int, minute: Int): String =
    hour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0')

fun formatReminderTime(schedule: ReminderSchedule): String =
    formatReminderTime(schedule.hour, schedule.minute)
