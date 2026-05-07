package com.chaos.bin.mt.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ReminderRepository(private val prefs: PreferenceRepository) {
    fun observe(): Flow<List<ReminderSchedule>> = prefs.observe(ReminderPreferenceKey)
        .map { ReminderJson.decode(it) }

    suspend fun list(): List<ReminderSchedule> = ReminderJson.decode(prefs.get(ReminderPreferenceKey))

    suspend fun save(items: List<ReminderSchedule>) {
        prefs.set(ReminderPreferenceKey, ReminderJson.encode(normalize(items)))
    }

    suspend fun add(hour: Int, minute: Int, enabled: Boolean = true): ReminderSchedule {
        val curr = list()
        check(curr.size < ReminderRules.MAX_COUNT) { "最多只能配置 ${ReminderRules.MAX_COUNT} 条提醒" }
        require(ReminderRules.isValidTime(hour, minute)) { "提醒时间非法" }
        val item = ReminderSchedule(ReminderRules.nextId(curr), hour, minute, enabled)
        save(curr + item)
        return item
    }

    suspend fun update(item: ReminderSchedule) {
        require(ReminderRules.isValidTime(item.hour, item.minute)) { "提醒时间非法" }
        val curr = list().map { if (it.id == item.id) item else it }
        save(curr)
    }

    suspend fun delete(id: Long) = save(list().filter { it.id != id })

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val curr = list().map { if (it.id == id) it.copy(enabled = enabled) else it }
        save(curr)
    }

    private fun normalize(items: List<ReminderSchedule>): List<ReminderSchedule> {
        require(items.size <= ReminderRules.MAX_COUNT) { "最多只能配置 ${ReminderRules.MAX_COUNT} 条提醒" }
        require(items.all { it.id > 0 && ReminderRules.isValidTime(it.hour, it.minute) }) { "提醒配置非法" }
        require(items.map { it.id }.distinct().size == items.size) { "提醒 ID 重复" }
        return items.sortedWith(compareBy<ReminderSchedule> { minutesOfDay(it.hour, it.minute) }.thenBy { it.id })
    }
}

internal const val ReminderPreferenceKey = "reminder.schedules"

internal object ReminderJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(items: List<ReminderSchedule>): String = json.encodeToString(items)

    fun decode(raw: String?): List<ReminderSchedule> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<ReminderSchedule>>(raw) }
            .getOrDefault(emptyList())
            .filter { it.id > 0 && ReminderRules.isValidTime(it.hour, it.minute) }
            .take(ReminderRules.MAX_COUNT)
    }
}
