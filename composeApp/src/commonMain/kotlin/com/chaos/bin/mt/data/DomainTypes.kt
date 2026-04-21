package com.chaos.bin.mt.data

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/** 记录类型。SQLDelight 里以 Int 存储：0=支出、1=收入。 */
enum class RecordKind(val code: Int) {
    Expense(0),
    Income(1);

    companion object {
        fun fromCode(code: Int): RecordKind = entries.first { it.code == code }
    }
}

/** 首页列表里每一行的投影（已连 Category / SubCategory / Account 的 name & emoji）。 */
data class RecordDetail(
    val id: Long,
    val kind: RecordKind,
    val amountCents: Long,
    val categoryId: String,
    val subCategoryId: String?,
    val accountId: String,
    val categoryName: String,
    val categoryEmoji: String,
    val categoryPrivacy: Boolean,
    val subCategoryName: String?,
    val subCategoryPrivacy: Boolean,
    val accountName: String,
    val accountEmoji: String,
    val note: String,
    val occurredAt: Instant,
    val privacy: Boolean,
) {
    /** 该条记录是否需要隐私遮蔽：记录自身、或所属小类、或所属大类任一带 privacy 标记。 */
    val effectivePrivacy: Boolean
        get() = privacy || subCategoryPrivacy || categoryPrivacy
}

/** 一天的分组。 */
data class DayGroup(
    val date: LocalDate,
    val items: List<RecordDetail>,
) {
    val expenseCentsSum: Long get() = items.filter { it.kind == RecordKind.Expense }.sumOf { it.amountCents }
}

/** 编辑时只需原始字段，没有 JOIN 出来的名字。 */
data class RawRecord(
    val id: Long,
    val kind: RecordKind,
    val amountCents: Long,
    val categoryId: String,
    val subCategoryId: String?,
    val accountId: String,
    val note: String,
    val occurredAt: Instant,
    val privacy: Boolean,
)

/** 当月汇总（单位：分）。 */
data class MonthSummary(
    val expenseCents: Long,
    val incomeCents: Long,
) {
    val balanceCents: Long get() = incomeCents - expenseCents
}
