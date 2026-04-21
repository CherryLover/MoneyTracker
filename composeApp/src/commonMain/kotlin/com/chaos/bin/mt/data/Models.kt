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

data class AutoRule(
    val id: Int,
    val name: String,
    val rule: String,
    val amount: Int,
    val type: RecordType,
    val cat: String,
    val account: String,
    val enabled: Boolean,
)
