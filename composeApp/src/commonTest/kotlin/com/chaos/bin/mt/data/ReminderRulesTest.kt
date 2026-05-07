package com.chaos.bin.mt.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReminderRulesTest {

    @Test
    fun conflictUsesAbsoluteGapWithoutMidnightWrap() {
        val existing = listOf(
            ReminderSchedule(id = 1, hour = 8, minute = 0),
            ReminderSchedule(id = 2, hour = 23, minute = 30),
        )
        assertSame(existing[0], ReminderRules.findConflict(8 to 59, existing))
        assertNull(ReminderRules.findConflict(9 to 0, existing))
        // 00:10 与 23:30 跨午夜只差 40 分钟，但规则明确不做 wrap，因此不冲突。
        assertNull(ReminderRules.findConflict(0 to 10, existing))
    }

    @Test
    fun conflictCanExcludeEditingItem() {
        val existing = listOf(
            ReminderSchedule(id = 1, hour = 8, minute = 0),
            ReminderSchedule(id = 2, hour = 21, minute = 30),
        )
        assertNull(ReminderRules.findConflict(8 to 30, existing, excludingId = 1))
        assertSame(existing[0], ReminderRules.findConflict(8 to 30, existing, excludingId = 2))
    }

    @Test
    fun idsAndTimeFormatting() {
        assertEquals(1L, ReminderRules.nextId(emptyList()))
        assertEquals(4L, ReminderRules.nextId(listOf(ReminderSchedule(3, 9, 0))))
        assertEquals("08:05", formatReminderTime(8, 5))
        assertTrue(ReminderRules.isValidTime(23, 59))
        assertFalse(ReminderRules.isValidTime(24, 0))
        assertFalse(ReminderRules.isValidTime(8, 60))
    }
}
