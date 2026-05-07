package com.chaos.bin.mt.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class ReminderTimeTest {

    @Test
    fun futureTodayReturnsToday() {
        val zone = TimeZone.UTC
        val now = millis(2026, 5, 7, 8, 0, zone)
        val got = computeNextTriggerMillis(9, 30, now, zone).asLocal(zone)
        assertEquals(LocalDateTime(2026, 5, 7, 9, 30, 0, 0), got)
    }

    @Test
    fun elapsedTodayReturnsTomorrowSameLocalTime() {
        val zone = TimeZone.UTC
        val now = millis(2026, 5, 7, 10, 0, zone)
        val got = computeNextTriggerMillis(9, 30, now, zone).asLocal(zone)
        assertEquals(LocalDateTime(2026, 5, 8, 9, 30, 0, 0), got)
    }

    @Test
    fun exactNowReturnsTomorrow() {
        val zone = TimeZone.UTC
        val now = millis(2026, 5, 7, 9, 30, zone)
        val got = computeNextTriggerMillis(9, 30, now, zone).asLocal(zone)
        assertEquals(LocalDateTime(2026, 5, 8, 9, 30, 0, 0), got)
    }

    @Test
    fun tomorrowCalculationKeepsLocalTimeAcrossDst() {
        val zone = TimeZone.of("America/New_York")
        val now = millis(2026, 3, 8, 10, 0, zone)
        val got = computeNextTriggerMillis(9, 0, now, zone).asLocal(zone)
        assertEquals(LocalDateTime(2026, 3, 9, 9, 0, 0, 0), got)
    }
}

private fun millis(y: Int, mo: Int, d: Int, h: Int, m: Int, zone: TimeZone): Long =
    LocalDateTime(y, mo, d, h, m, 0, 0).toInstant(zone).toEpochMilliseconds()

private fun Long.asLocal(zone: TimeZone): LocalDateTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(zone)
