package com.chaos.bin.mt.data

import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

/** 导入服务：把 CSV 原始行按名字查 ID、校验格式、写入 RecordRepository。 */
class CsvImportService(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val recordRepository: RecordRepository,
) {

    suspend fun importCsv(
        content: String,
        zone: TimeZone = AppTimeZone,
    ): ImportResult {
        val rawRows = try {
            parseCsv(content)
        } catch (e: IllegalArgumentException) {
            return ImportResult(0, 0, listOf("解析失败: ${e.message}"))
        }
        if (rawRows.isEmpty()) return ImportResult(0, 0, emptyList())

        val expenseTree = categoryRepository.observeTree(RecordKind.Expense).first()
        val incomeTree = categoryRepository.observeTree(RecordKind.Income).first()
        val accounts = accountRepository.observeAll().first()

        var inserted = 0
        var skipped = 0
        val errors = mutableListOf<String>()

        rawRows.forEachIndexed { index, row ->
            val lineNo = index + 2 // header 之后第一行 = 2
            val kind = when (row.typeText) {
                CsvExpenseLabel -> RecordKind.Expense
                CsvIncomeLabel -> RecordKind.Income
                else -> {
                    skipped++
                    errors += "第 $lineNo 行: 未知类型 '${row.typeText}'"
                    return@forEachIndexed
                }
            }

            val occurredMillis = parseDateTime(row.dateText, zone)
            if (occurredMillis == null) {
                skipped++
                errors += "第 $lineNo 行: 日期格式错误 '${row.dateText}'"
                return@forEachIndexed
            }

            val amountCents = parseAmountToCents(row.amountText)
            if (amountCents == null) {
                skipped++
                errors += "第 $lineNo 行: 金额格式错误 '${row.amountText}'"
                return@forEachIndexed
            }

            val tree = if (kind == RecordKind.Expense) expenseTree else incomeTree
            val category = tree.firstOrNull { it.name == row.categoryName }
            if (category == null) {
                skipped++
                errors += "第 $lineNo 行: 未找到大类 '${row.categoryName}'"
                return@forEachIndexed
            }

            val subCategoryId: String? = if (row.subCategoryName.isEmpty()) {
                null
            } else {
                val sub = category.subs.firstOrNull { it.name == row.subCategoryName }
                if (sub == null) {
                    skipped++
                    errors += "第 $lineNo 行: 未找到小类 '${row.subCategoryName}'"
                    return@forEachIndexed
                }
                sub.id
            }

            val account = accounts.firstOrNull { it.name == row.accountName }
            if (account == null) {
                skipped++
                errors += "第 $lineNo 行: 未找到账户 '${row.accountName}'"
                return@forEachIndexed
            }

            recordRepository.insert(
                kind = kind,
                amountCents = amountCents,
                categoryId = category.id,
                subCategoryId = subCategoryId,
                accountId = account.id,
                note = row.note,
                occurredAt = Instant.fromEpochMilliseconds(occurredMillis),
                privacy = false,
            )
            inserted++
        }

        return ImportResult(inserted, skipped, errors)
    }
}
