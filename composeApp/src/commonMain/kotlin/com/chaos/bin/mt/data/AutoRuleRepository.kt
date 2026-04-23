package com.chaos.bin.mt.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.chaos.bin.mt.db.MtDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AutoRuleRepository(private val db: MtDatabase) {

    fun observeAll(): Flow<List<AutoRule>> =
        db.autoRuleQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    fun observeEnabled(): Flow<List<AutoRule>> =
        db.autoRuleQueries.selectEnabled()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    suspend fun getById(id: Long): AutoRule? = withContext(Dispatchers.Default) {
        db.autoRuleQueries.selectById(id).executeAsOneOrNull()?.toDomain()
    }

    suspend fun listEnabled(): List<AutoRule> = withContext(Dispatchers.Default) {
        db.autoRuleQueries.selectEnabled().executeAsList().map { it.toDomain() }
    }

    suspend fun insert(rule: AutoRule): Long = withContext(Dispatchers.Default) {
        val cols = rule.trigger.toColumns()
        db.autoRuleQueries.insert(
            name = rule.name,
            kind = rule.kind.code.toLong(),
            amount_cents = rule.amountCents,
            category_id = rule.categoryId,
            sub_category_id = rule.subCategoryId,
            account_id = rule.accountId,
            note = rule.note,
            trigger_type = cols.type.toLong(),
            trigger_weekdays = cols.weekdays?.toLong(),
            trigger_month_days = cols.monthDays?.toLong(),
            trigger_interval_days = cols.intervalDays?.toLong(),
            trigger_hour = rule.trigger.hour.toLong(),
            trigger_minute = rule.trigger.minute.toLong(),
            enabled = if (rule.enabled) 1 else 0,
            last_fired_at = rule.lastFiredAt,
            created_at = rule.createdAt,
            updated_at = rule.updatedAt,
        )
        db.autoRuleQueries.lastInsertRowId().executeAsOne()
    }

    suspend fun update(rule: AutoRule) = withContext(Dispatchers.Default) {
        val cols = rule.trigger.toColumns()
        db.autoRuleQueries.update(
            name = rule.name,
            kind = rule.kind.code.toLong(),
            amount_cents = rule.amountCents,
            category_id = rule.categoryId,
            sub_category_id = rule.subCategoryId,
            account_id = rule.accountId,
            note = rule.note,
            trigger_type = cols.type.toLong(),
            trigger_weekdays = cols.weekdays?.toLong(),
            trigger_month_days = cols.monthDays?.toLong(),
            trigger_interval_days = cols.intervalDays?.toLong(),
            trigger_hour = rule.trigger.hour.toLong(),
            trigger_minute = rule.trigger.minute.toLong(),
            enabled = if (rule.enabled) 1 else 0,
            updated_at = nowMillis(),
            id = rule.id,
        )
    }

    suspend fun updateEnabled(id: Long, enabled: Boolean) = withContext(Dispatchers.Default) {
        db.autoRuleQueries.updateEnabled(
            enabled = if (enabled) 1 else 0,
            updated_at = nowMillis(),
            id = id,
        )
    }

    suspend fun updateLastFired(id: Long, lastFiredAt: Long) = withContext(Dispatchers.Default) {
        db.autoRuleQueries.updateLastFired(
            last_fired_at = lastFiredAt,
            updated_at = nowMillis(),
            id = id,
        )
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.Default) {
        db.autoRuleQueries.deleteById(id)
    }
}

private data class TriggerColumns(
    val type: Int,
    val weekdays: Int?,
    val monthDays: Int?,
    val intervalDays: Int?,
)

private fun TriggerConfig.toColumns(): TriggerColumns = when (this) {
    is TriggerConfig.Weekly -> TriggerColumns(0, weekdaysMask, null, null)
    is TriggerConfig.MonthlyDays -> TriggerColumns(1, null, daysMask, null)
    is TriggerConfig.Interval -> TriggerColumns(2, null, null, intervalDays)
}

private fun com.chaos.bin.mt.db.AutoRule.toDomain(): AutoRule {
    val hour = trigger_hour.toInt()
    val minute = trigger_minute.toInt()
    val trigger: TriggerConfig = when (trigger_type.toInt()) {
        0 -> TriggerConfig.Weekly(
            weekdaysMask = (trigger_weekdays ?: 0L).toInt(),
            hour = hour,
            minute = minute,
        )
        1 -> TriggerConfig.MonthlyDays(
            daysMask = (trigger_month_days ?: 0L).toInt(),
            hour = hour,
            minute = minute,
        )
        2 -> TriggerConfig.Interval(
            intervalDays = (trigger_interval_days ?: 1L).toInt().coerceAtLeast(1),
            hour = hour,
            minute = minute,
        )
        else -> TriggerConfig.Weekly(0, hour, minute)
    }
    return AutoRule(
        id = id,
        name = name,
        kind = RecordKind.fromCode(kind.toInt()),
        amountCents = amount_cents,
        categoryId = category_id,
        subCategoryId = sub_category_id,
        accountId = account_id,
        note = note,
        trigger = trigger,
        enabled = enabled != 0L,
        lastFiredAt = last_fired_at,
        createdAt = created_at,
        updatedAt = updated_at,
    )
}
