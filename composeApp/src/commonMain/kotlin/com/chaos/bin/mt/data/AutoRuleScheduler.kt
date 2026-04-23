package com.chaos.bin.mt.data

import kotlin.time.ExperimentalTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * 启动时对所有启用规则做一次追赶：枚举 (last_fired_at, now] 内所有触发时刻，
 * 逐个 insert Record，最后把 last_fired_at 推进到最后一次触发时刻。
 */
class AutoRuleScheduler(
    private val autoRuleRepository: AutoRuleRepository,
    private val recordRepository: RecordRepository,
    private val zone: TimeZone = AppTimeZone,
) {

    @OptIn(ExperimentalTime::class)
    suspend fun catchUp() {
        val now = nowMillis()
        val rules = autoRuleRepository.listEnabled()
        for (rule in rules) {
            val fromExclusive = rule.lastFiredAt ?: rule.createdAt
            if (fromExclusive >= now) continue
            val triggers = enumerateTriggers(rule.trigger, fromExclusive, now, zone)
            if (triggers.isEmpty()) continue
            for (millis in triggers) {
                recordRepository.insert(
                    kind = rule.kind,
                    amountCents = rule.amountCents,
                    categoryId = rule.categoryId,
                    subCategoryId = rule.subCategoryId,
                    accountId = rule.accountId,
                    note = rule.note,
                    occurredAt = Instant.fromEpochMilliseconds(millis),
                    privacy = false,
                    source = 1,
                    autoRuleId = rule.id,
                )
            }
            autoRuleRepository.updateLastFired(rule.id, triggers.last())
        }
    }
}

/**
 * 枚举 [trigger] 在 `(fromExclusive, toInclusive]` 区间内的所有触发时刻（epoch millis，升序）。
 * 所有日期推演都在 [zone] 本地时区里进行。
 */
fun enumerateTriggers(
    trigger: TriggerConfig,
    fromExclusive: Long,
    toInclusive: Long,
    zone: TimeZone = AppTimeZone,
): List<Long> {
    if (fromExclusive >= toInclusive) return emptyList()
    return when (trigger) {
        is TriggerConfig.Weekly -> enumerateWeekly(trigger, fromExclusive, toInclusive, zone)
        is TriggerConfig.MonthlyDays -> enumerateMonthly(trigger, fromExclusive, toInclusive, zone)
        is TriggerConfig.Interval -> enumerateInterval(trigger, fromExclusive, toInclusive, zone)
    }
}

private fun enumerateWeekly(
    t: TriggerConfig.Weekly,
    fromExclusive: Long,
    toInclusive: Long,
    zone: TimeZone,
): List<Long> {
    if (t.weekdaysMask == 0) return emptyList()
    val out = mutableListOf<Long>()
    val startDate = Instant.fromEpochMilliseconds(fromExclusive).toLocalDateTime(zone).date
    val endDate = Instant.fromEpochMilliseconds(toInclusive).toLocalDateTime(zone).date
    var d = startDate
    while (d <= endDate) {
        val bit = dayOfWeekBit(d.dayOfWeek)
        if ((t.weekdaysMask and (1 shl bit)) != 0) {
            val millis = atTimeMillis(d, t.hour, t.minute, zone)
            if (millis in (fromExclusive + 1)..toInclusive) out.add(millis)
        }
        d = d.plus(1, DateTimeUnit.DAY)
    }
    return out
}

private fun enumerateMonthly(
    t: TriggerConfig.MonthlyDays,
    fromExclusive: Long,
    toInclusive: Long,
    zone: TimeZone,
): List<Long> {
    if (t.daysMask == 0) return emptyList()
    val out = mutableListOf<Long>()
    val startDate = Instant.fromEpochMilliseconds(fromExclusive).toLocalDateTime(zone).date
    val endDate = Instant.fromEpochMilliseconds(toInclusive).toLocalDateTime(zone).date
    var d = startDate
    while (d <= endDate) {
        val bit = d.dayOfMonth - 1
        if (bit in 0..30 && (t.daysMask and (1 shl bit)) != 0) {
            val millis = atTimeMillis(d, t.hour, t.minute, zone)
            if (millis in (fromExclusive + 1)..toInclusive) out.add(millis)
        }
        d = d.plus(1, DateTimeUnit.DAY)
    }
    return out
}

private fun enumerateInterval(
    t: TriggerConfig.Interval,
    fromExclusive: Long,
    toInclusive: Long,
    zone: TimeZone,
): List<Long> {
    val n = t.intervalDays.coerceAtLeast(1)
    val out = mutableListOf<Long>()
    // 基准日期为 fromExclusive 当日，每次 + N 天。第一个候选是 fromDate + N days。
    val fromDate = Instant.fromEpochMilliseconds(fromExclusive).toLocalDateTime(zone).date
    val endDate = Instant.fromEpochMilliseconds(toInclusive).toLocalDateTime(zone).date
    var d = fromDate.plus(n, DateTimeUnit.DAY)
    while (d <= endDate) {
        val millis = atTimeMillis(d, t.hour, t.minute, zone)
        if (millis in (fromExclusive + 1)..toInclusive) out.add(millis)
        d = d.plus(n, DateTimeUnit.DAY)
    }
    // 处理边界：若 fromDate 当天的 HH:mm 晚于 fromExclusive 且 <= to，可能也应算第一个。
    // 但按"每隔 N 天"的定义，首次应该在 last_fired + N 天后，这里不补 day0。
    return out
}

/** kotlinx-datetime 的 MONDAY=1..SUNDAY=7 → bit0=周日..bit6=周六。 */
internal fun dayOfWeekBit(dow: DayOfWeek): Int = when (dow) {
    DayOfWeek.SUNDAY -> 0
    DayOfWeek.MONDAY -> 1
    DayOfWeek.TUESDAY -> 2
    DayOfWeek.WEDNESDAY -> 3
    DayOfWeek.THURSDAY -> 4
    DayOfWeek.FRIDAY -> 5
    DayOfWeek.SATURDAY -> 6
    else -> 0
}

private fun atTimeMillis(date: LocalDate, hour: Int, minute: Int, zone: TimeZone): Long {
    val ldt = LocalDateTime(
        year = date.year,
        monthNumber = date.monthNumber,
        dayOfMonth = date.dayOfMonth,
        hour = hour,
        minute = minute,
        second = 0,
        nanosecond = 0,
    )
    return ldt.toInstant(zone).toEpochMilliseconds()
}

@Suppress("unused")
private fun atStartOfDayMillis(date: LocalDate, zone: TimeZone): Long =
    date.atStartOfDayIn(zone).toEpochMilliseconds()
