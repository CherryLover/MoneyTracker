package com.chaos.bin.mt.data

enum class RecordType { Expense, Income }

data class SubCategory(
    val id: String,
    val name: String,
    val privacy: Boolean = false,
)

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val privacy: Boolean = false,
    val subs: List<SubCategory> = emptyList(),
)

data class Account(
    val id: String,
    val name: String,
    val emoji: String,
)

data class MoneyRecord(
    val id: Int,
    val type: RecordType,
    val cat: String,
    val sub: String,
    val emoji: String,
    val amount: Int,
    val note: String = "",
    val account: String,
    val time: String,
    val privacy: Boolean = false,
)

data class DayRecords(
    val day: Int,
    val weekday: String,
    val items: List<MoneyRecord>,
)

/** 自动记账触发配置。type 由 sealed class 具体类决定。 */
sealed class TriggerConfig {
    abstract val hour: Int
    abstract val minute: Int

    /** 每周：weekdaysMask 的 bit0=周日..bit6=周六。 */
    data class Weekly(
        val weekdaysMask: Int,
        override val hour: Int,
        override val minute: Int,
    ) : TriggerConfig()

    /** 每月：daysMask 的 bit0..bit30 对应 1..31 号；没有的天自动跳过。 */
    data class MonthlyDays(
        val daysMask: Int,
        override val hour: Int,
        override val minute: Int,
    ) : TriggerConfig()

    /** 每隔 N 天：按本地时区日期加 N 天，落在 HH:mm。 */
    data class Interval(
        val intervalDays: Int,
        override val hour: Int,
        override val minute: Int,
    ) : TriggerConfig()
}

data class AutoRule(
    val id: Long,
    val name: String,
    val kind: RecordKind,
    val amountCents: Long,
    val categoryId: String,
    val subCategoryId: String?,
    val accountId: String,
    val note: String,
    val trigger: TriggerConfig,
    val enabled: Boolean,
    val lastFiredAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
