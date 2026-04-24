package com.chaos.bin.mt.data

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** 从 CSV 里原样解析出来的一行；ID 查找放在 CsvImportService 里做。 */
data class CsvRow(
    val dateText: String,
    val typeText: String,
    val categoryName: String,
    val subCategoryName: String,
    val amountText: String,
    val accountName: String,
    val note: String,
)

/** 导入结果。errors 里每条为 `第 N 行: 原因`。 */
data class ImportResult(
    val insertedCount: Int,
    val skippedCount: Int,
    val errors: List<String>,
)

internal val CsvHeader = listOf("日期", "类型", "大类", "小类", "金额", "账户", "备注")
internal const val CsvExpenseLabel = "支出"
internal const val CsvIncomeLabel = "收入"

/** 把记录导出为 CSV 文本（UTF-8，LF 换行，RFC 4180 转义）。 */
fun formatCsv(records: List<RecordDetail>, zone: TimeZone = AppTimeZone): String {
    val sb = StringBuilder()
    sb.append(CsvHeader.joinToString(",") { escapeCsvField(it) })
    sb.append('\n')
    for (r in records) {
        val ldt = r.occurredAt.toLocalDateTime(zone)
        val fields = listOf(
            formatDateTime(ldt),
            if (r.kind == RecordKind.Expense) CsvExpenseLabel else CsvIncomeLabel,
            r.categoryName,
            r.subCategoryName ?: "",
            formatAmountCents(r.amountCents),
            r.accountName,
            r.note,
        )
        sb.append(fields.joinToString(",") { escapeCsvField(it) })
        sb.append('\n')
    }
    return sb.toString()
}

/** 解析 CSV 文本；遇到格式错误的行会抛 IllegalArgumentException。 */
fun parseCsv(content: String): List<CsvRow> {
    val allRows = parseCsvRaw(content)
    if (allRows.isEmpty()) return emptyList()
    // 跳过 header 行；不严格校验表头，只要第一行字段数 >= 7 就按表头处理
    val dataRows = if (allRows[0].size >= 7 && allRows[0][0].trim() == CsvHeader[0]) allRows.drop(1) else allRows
    return dataRows.mapNotNull { fields ->
        if (fields.all { it.isEmpty() }) return@mapNotNull null // 空行
        if (fields.size < 7) {
            throw IllegalArgumentException("字段数不足（需要 7 列，实际 ${fields.size} 列）")
        }
        CsvRow(
            dateText = fields[0].trim(),
            typeText = fields[1].trim(),
            categoryName = fields[2].trim(),
            subCategoryName = fields[3].trim(),
            amountText = fields[4].trim(),
            accountName = fields[5].trim(),
            note = fields[6],
        )
    }
}

/** RFC 4180 转义：包含 `,` / `"` / `\n` / `\r` 时加双引号，内部 `"` 转义为 `""`。 */
internal fun escapeCsvField(v: String): String {
    val needQuote = v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    if (!needQuote) return v
    return "\"" + v.replace("\"", "\"\"") + "\""
}

/** 把整块 CSV 文本切成二维字符串；处理引号内换行与 `""` 转义。 */
internal fun parseCsvRaw(content: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val field = StringBuilder()
    var i = 0
    var inQuotes = false
    while (i < content.length) {
        val ch = content[i]
        if (inQuotes) {
            if (ch == '"') {
                if (i + 1 < content.length && content[i + 1] == '"') {
                    field.append('"')
                    i += 2
                    continue
                }
                inQuotes = false
                i++
                continue
            }
            field.append(ch)
            i++
        } else {
            when (ch) {
                '"' -> {
                    inQuotes = true
                    i++
                }
                ',' -> {
                    row.add(field.toString())
                    field.setLength(0)
                    i++
                }
                '\r' -> {
                    // 吞掉 \r\n 中的 \r
                    i++
                }
                '\n' -> {
                    row.add(field.toString())
                    rows.add(row.toList())
                    row.clear()
                    field.setLength(0)
                    i++
                }
                else -> {
                    field.append(ch)
                    i++
                }
            }
        }
    }
    // 收尾：最后一行可能没有换行
    if (field.isNotEmpty() || row.isNotEmpty()) {
        row.add(field.toString())
        rows.add(row.toList())
    }
    return rows
}

internal fun formatDateTime(ldt: LocalDateTime): String {
    val y = ldt.year.toString().padStart(4, '0')
    val mo = ldt.monthNumber.toString().padStart(2, '0')
    val d = ldt.dayOfMonth.toString().padStart(2, '0')
    val h = ldt.hour.toString().padStart(2, '0')
    val mi = ldt.minute.toString().padStart(2, '0')
    return "$y-$mo-$d $h:$mi"
}

/** 严格格式 `YYYY-MM-DD HH:mm`。格式或数值非法返回 null。 */
internal fun parseDateTime(text: String, zone: TimeZone): Long? {
    if (text.length != 16) return null
    if (text[4] != '-' || text[7] != '-' || text[10] != ' ' || text[13] != ':') return null
    val y = text.substring(0, 4).toIntOrNull() ?: return null
    val mo = text.substring(5, 7).toIntOrNull() ?: return null
    val d = text.substring(8, 10).toIntOrNull() ?: return null
    val h = text.substring(11, 13).toIntOrNull() ?: return null
    val mi = text.substring(14, 16).toIntOrNull() ?: return null
    return try {
        LocalDateTime(y, mo, d, h, mi, 0, 0).toInstant(zone).toEpochMilliseconds()
    } catch (_: IllegalArgumentException) {
        null
    }
}

internal fun formatAmountCents(cents: Long): String {
    val abs = if (cents < 0) -cents else cents
    val yuan = abs / 100
    val rem = (abs % 100).toString().padStart(2, '0')
    val sign = if (cents < 0) "-" else ""
    return "$sign$yuan.$rem"
}

/** 解析 "35" / "35.5" / "35.50" / "12345.67" → cents。不接受千分位。 */
internal fun parseAmountToCents(text: String): Long? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    val negative = trimmed.startsWith("-")
    val body = if (negative) trimmed.substring(1) else trimmed
    if (body.isEmpty() || body.contains(",")) return null
    val dot = body.indexOf('.')
    val intPart: String
    val fracPart: String
    if (dot < 0) {
        intPart = body
        fracPart = "00"
    } else {
        intPart = body.substring(0, dot)
        val raw = body.substring(dot + 1)
        if (raw.isEmpty() || raw.length > 2) return null
        fracPart = if (raw.length == 1) raw + "0" else raw
    }
    if (intPart.isEmpty() || !intPart.all { it.isDigit() }) return null
    if (!fracPart.all { it.isDigit() }) return null
    val yuan = intPart.toLongOrNull() ?: return null
    val cents = fracPart.toLongOrNull() ?: return null
    val total = yuan * 100 + cents
    return if (negative) -total else total
}
