package com.chaos.bin.mt.data

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun computeNextTriggerMillis(
    hour: Int,
    minute: Int,
    nowMillis: Long = nowMillis(),
    zone: TimeZone = AppTimeZone,
): Long {
    require(ReminderRules.isValidTime(hour, minute)) { "提醒时间非法" }
    val nowLocal = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(zone)
    val today = nowLocal.date.atReminderTimeMillis(hour, minute, zone)
    if (today > nowMillis) return today
    val tomorrow = nowLocal.date.plus(1, DateTimeUnit.DAY)
    return tomorrow.atReminderTimeMillis(hour, minute, zone)
}

private fun LocalDate.atReminderTimeMillis(hour: Int, minute: Int, zone: TimeZone): Long =
    LocalDateTime(year, monthNumber, dayOfMonth, hour, minute, 0, 0)
        .toInstant(zone)
        .toEpochMilliseconds()
