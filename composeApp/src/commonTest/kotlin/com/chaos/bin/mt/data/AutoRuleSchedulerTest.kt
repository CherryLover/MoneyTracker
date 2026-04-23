package com.chaos.bin.mt.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private val UTC = TimeZone.UTC

private fun millis(y: Int, mo: Int, d: Int, h: Int = 0, m: Int = 0): Long =
    LocalDateTime(y, mo, d, h, m, 0, 0).toInstant(UTC).toEpochMilliseconds()

private fun Long.asLdt(): LocalDateTime =
    kotlinx.datetime.Instant.fromEpochMilliseconds(this).toLocalDateTime(UTC)

class AutoRuleSchedulerTest {

    @Test
    fun emptyRangeReturnsEmpty() {
        val t = TriggerConfig.Weekly(weekdaysMask = 0b1111111, hour = 9, minute = 0)
        val now = millis(2026, 4, 23, 12, 0)
        assertTrue(enumerateTriggers(t, now, now, UTC).isEmpty())
        assertTrue(enumerateTriggers(t, now + 10, now, UTC).isEmpty())
    }

    @Test
    fun weeklySingleDay() {
        // 2026-04-23 是周四。bit4 = 周四
        val t = TriggerConfig.Weekly(weekdaysMask = 1 shl 4, hour = 9, minute = 0)
        // from 2026-04-22 00:00 → to 2026-05-07 23:59 (含 5-7 09:00)
        val from = millis(2026, 4, 22, 0, 0)
        val to = millis(2026, 5, 7, 23, 59)
        val got = enumerateTriggers(t, from, to, UTC)
        // 触发日：4-23, 4-30, 5-7
        assertEquals(3, got.size)
        assertEquals(23, got[0].asLdt().dayOfMonth)
        assertEquals(30, got[1].asLdt().dayOfMonth)
        assertEquals(5, got[2].asLdt().monthNumber)
        assertEquals(7, got[2].asLdt().dayOfMonth)
        got.forEach {
            assertEquals(9, it.asLdt().hour)
            assertEquals(0, it.asLdt().minute)
        }
    }

    @Test
    fun weeklyMultipleDays() {
        // 周一..周五 = bit1..bit5
        val mask = (1 shl 1) or (1 shl 2) or (1 shl 3) or (1 shl 4) or (1 shl 5)
        val t = TriggerConfig.Weekly(weekdaysMask = mask, hour = 9, minute = 0)
        // 2026-04-20 (周一) 到 2026-04-26 (周日)
        val from = millis(2026, 4, 19, 23, 0)
        val to = millis(2026, 4, 27, 0, 0)
        val got = enumerateTriggers(t, from, to, UTC)
        // 4-20..4-24 共 5 天
        assertEquals(5, got.size)
        assertEquals(listOf(20, 21, 22, 23, 24), got.map { it.asLdt().dayOfMonth })
    }

    @Test
    fun weeklyCrossesWeekBoundary() {
        // 周日 + 周三 = bit0 + bit3
        val mask = (1 shl 0) or (1 shl 3)
        val t = TriggerConfig.Weekly(weekdaysMask = mask, hour = 8, minute = 30)
        // 2026-04-15 (周三) 到 2026-04-27 (周一)
        val from = millis(2026, 4, 14, 0, 0)
        val to = millis(2026, 4, 27, 0, 0)
        val got = enumerateTriggers(t, from, to, UTC)
        // 4-15 周三, 4-19 周日, 4-22 周三, 4-26 周日
        assertEquals(4, got.size)
        assertEquals(listOf(15, 19, 22, 26), got.map { it.asLdt().dayOfMonth })
    }

    @Test
    fun monthlyDaysSkipsMissingInFebruary() {
        // 选择 30 和 31 号 → bit29 + bit30
        val mask = (1 shl 29) or (1 shl 30)
        val t = TriggerConfig.MonthlyDays(daysMask = mask, hour = 0, minute = 0)
        // 2026 是非闰年 → 2 月 28 天
        val from = millis(2026, 2, 1, 0, 0)
        val to = millis(2026, 2, 28, 23, 59)
        val got = enumerateTriggers(t, from, to, UTC)
        assertTrue(got.isEmpty())
    }

    @Test
    fun monthlyDaysIncludesExistingDays() {
        // 选 1 号 + 15 号 + 31 号 → bit0 + bit14 + bit30
        val mask = (1 shl 0) or (1 shl 14) or (1 shl 30)
        val t = TriggerConfig.MonthlyDays(daysMask = mask, hour = 10, minute = 0)
        val from = millis(2026, 3, 31, 23, 59)
        val to = millis(2026, 5, 31, 23, 59)
        val got = enumerateTriggers(t, from, to, UTC)
        // 期望: 4-1, 4-15, 5-1, 5-15, 5-31（4 月没有 31 号）
        val keys = got.map { it.asLdt().let { d -> d.monthNumber to d.dayOfMonth } }
        assertEquals(
            listOf(4 to 1, 4 to 15, 5 to 1, 5 to 15, 5 to 31),
            keys,
        )
    }

    @Test
    fun intervalDaily() {
        val t = TriggerConfig.Interval(intervalDays = 1, hour = 12, minute = 0)
        val from = millis(2026, 4, 20, 12, 0)
        val to = millis(2026, 4, 25, 12, 0)
        val got = enumerateTriggers(t, from, to, UTC)
        // 第一次是 4-21 12:00，然后 22/23/24/25
        assertEquals(5, got.size)
        assertEquals(listOf(21, 22, 23, 24, 25), got.map { it.asLdt().dayOfMonth })
    }

    @Test
    fun intervalEveryWeek() {
        val t = TriggerConfig.Interval(intervalDays = 7, hour = 9, minute = 0)
        val from = millis(2026, 4, 1, 9, 0)
        val to = millis(2026, 5, 1, 9, 0)
        val got = enumerateTriggers(t, from, to, UTC)
        // 4-8, 4-15, 4-22, 4-29
        assertEquals(4, got.size)
        assertEquals(listOf(8, 15, 22, 29), got.map { it.asLdt().dayOfMonth })
    }

    @Test
    fun weeklyZeroMaskNoTriggers() {
        val t = TriggerConfig.Weekly(weekdaysMask = 0, hour = 9, minute = 0)
        val from = millis(2026, 4, 1, 0, 0)
        val to = millis(2026, 5, 1, 0, 0)
        assertTrue(enumerateTriggers(t, from, to, UTC).isEmpty())
    }

    @Test
    fun monthlyZeroMaskNoTriggers() {
        val t = TriggerConfig.MonthlyDays(daysMask = 0, hour = 9, minute = 0)
        val from = millis(2026, 4, 1, 0, 0)
        val to = millis(2026, 5, 1, 0, 0)
        assertTrue(enumerateTriggers(t, from, to, UTC).isEmpty())
    }

    @Test
    fun catchUpFallbackUsesCreatedAtWhenLastFiredAtIsNull() {
        val t = TriggerConfig.Weekly(weekdaysMask = 0b1111111, hour = 9, minute = 0)
        val createdAt = millis(2026, 4, 1, 0, 0)
        val now = millis(2026, 4, 5, 23, 59)

        val fromCreatedAt = enumerateTriggers(t, createdAt, now, UTC)
        assertEquals(listOf(1, 2, 3, 4, 5), fromCreatedAt.map { it.asLdt().dayOfMonth })

        val lastFiredAt: Long? = null
        val fromExclusive = lastFiredAt ?: createdAt
        assertEquals(createdAt, fromExclusive)
        assertEquals(fromCreatedAt, enumerateTriggers(t, fromExclusive, now, UTC))
    }
}
