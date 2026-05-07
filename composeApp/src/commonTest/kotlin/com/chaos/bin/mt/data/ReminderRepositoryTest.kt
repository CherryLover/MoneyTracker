package com.chaos.bin.mt.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderRepositoryTest {

    @Test
    fun jsonRoundTripIncludesDefaultsAndSortsViaRepositoryFormat() {
        val raw = listOf(
            ReminderSchedule(id = 2, hour = 21, minute = 30, enabled = false),
            ReminderSchedule(id = 1, hour = 8, minute = 0),
        )
        val encoded = ReminderJson.encode(raw)
        val decoded = ReminderJson.decode(encoded)
        assertEquals(raw, decoded)
        assertTrue(encoded.contains("\"enabled\":true"))
    }

    @Test
    fun jsonDecodeToleratesEmptyBrokenAndUnknownFields() {
        assertEquals(emptyList(), ReminderJson.decode(null))
        assertEquals(emptyList(), ReminderJson.decode(""))
        assertEquals(emptyList(), ReminderJson.decode("not-json"))

        val decoded = ReminderJson.decode(
            """
            [
              {"id":1,"hour":8,"minute":0,"enabled":true,"future":"ok"},
              {"id":2,"hour":24,"minute":0,"enabled":true},
              {"id":3,"hour":9,"minute":60,"enabled":true},
              {"id":4,"hour":21,"minute":30,"enabled":false}
            ]
            """.trimIndent(),
        )
        assertEquals(
            listOf(
                ReminderSchedule(1, 8, 0, true),
                ReminderSchedule(4, 21, 30, false),
            ),
            decoded,
        )
    }

    @Test
    fun jsonDecodeCapsAtMaxCount() {
        val decoded = ReminderJson.decode(
            """
            [
              {"id":1,"hour":8,"minute":0,"enabled":true},
              {"id":2,"hour":10,"minute":0,"enabled":true},
              {"id":3,"hour":12,"minute":0,"enabled":true},
              {"id":4,"hour":14,"minute":0,"enabled":true}
            ]
            """.trimIndent(),
        )
        assertEquals(3, decoded.size)
        assertEquals(listOf(1L, 2L, 3L), decoded.map { it.id })
    }
}
