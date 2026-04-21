package com.chaos.bin.mt.data

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/** App 统一使用本地时区计算"月"边界与分组。 */
val AppTimeZone: TimeZone = TimeZone.currentSystemDefault()

fun Instant.toLocalDate(): LocalDate = this.toLocalDateTime(AppTimeZone).date

@OptIn(ExperimentalTime::class)
fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

@OptIn(ExperimentalTime::class)
fun nowInstant(): Instant = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())

/** 返回 [year]-[month] 这个自然月 `[startMillis, endMillis)` 边界。 */
fun monthRangeMillis(year: Int, month: Int): Pair<Long, Long> {
    val start = LocalDate(year, month, 1).atStartOfDayIn(AppTimeZone).toEpochMilliseconds()
    val nextMonth = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
    val end = nextMonth.atStartOfDayIn(AppTimeZone).toEpochMilliseconds()
    return start to end
}
