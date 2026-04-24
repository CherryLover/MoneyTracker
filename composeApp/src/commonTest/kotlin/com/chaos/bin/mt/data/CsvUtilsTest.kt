package com.chaos.bin.mt.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val UTC = TimeZone.UTC

private fun recordOf(
    kind: RecordKind,
    amountCents: Long,
    categoryName: String,
    subCategoryName: String?,
    accountName: String,
    note: String,
    ldt: LocalDateTime,
): RecordDetail = RecordDetail(
    id = 0,
    kind = kind,
    amountCents = amountCents,
    categoryId = "cat",
    subCategoryId = if (subCategoryName == null) null else "sub",
    accountId = "acc",
    categoryName = categoryName,
    categoryEmoji = "",
    categoryPrivacy = false,
    subCategoryName = subCategoryName,
    subCategoryPrivacy = false,
    accountName = accountName,
    accountEmoji = "",
    note = note,
    occurredAt = ldt.toInstant(UTC),
    privacy = false,
)

class CsvUtilsTest {

    @Test
    fun escapeSimpleFieldsUnchanged() {
        assertEquals("hello", escapeCsvField("hello"))
        assertEquals("", escapeCsvField(""))
        assertEquals("餐饮", escapeCsvField("餐饮"))
    }

    @Test
    fun escapeQuotesCommasAndNewlines() {
        assertEquals("\"a,b\"", escapeCsvField("a,b"))
        assertEquals("\"he said \"\"hi\"\"\"", escapeCsvField("he said \"hi\""))
        assertEquals("\"line1\nline2\"", escapeCsvField("line1\nline2"))
        assertEquals("\"line1\rline2\"", escapeCsvField("line1\rline2"))
    }

    @Test
    fun parseRawHandlesEscapedFields() {
        val content = "a,b,c\n\"x,y\",\"he said \"\"hi\"\"\",\"z\"\n"
        val rows = parseCsvRaw(content)
        assertEquals(2, rows.size)
        assertEquals(listOf("a", "b", "c"), rows[0])
        assertEquals(listOf("x,y", "he said \"hi\"", "z"), rows[1])
    }

    @Test
    fun parseRawHandlesQuotedNewline() {
        val content = "a,b\n\"line1\nline2\",b\n"
        val rows = parseCsvRaw(content)
        assertEquals(2, rows.size)
        assertEquals(listOf("line1\nline2", "b"), rows[1])
    }

    @Test
    fun parseRawHandlesCrlf() {
        val content = "a,b\r\nc,d\r\n"
        val rows = parseCsvRaw(content)
        assertEquals(2, rows.size)
        assertEquals(listOf("c", "d"), rows[1])
    }

    @Test
    fun amountRoundTrip() {
        assertEquals("35.00", formatAmountCents(3500))
        assertEquals("0.50", formatAmountCents(50))
        assertEquals("12345.67", formatAmountCents(1234567))
        assertEquals("-3.14", formatAmountCents(-314))

        assertEquals(3500L, parseAmountToCents("35"))
        assertEquals(3550L, parseAmountToCents("35.5"))
        assertEquals(3550L, parseAmountToCents("35.50"))
        assertEquals(1234567L, parseAmountToCents("12345.67"))
        assertEquals(-314L, parseAmountToCents("-3.14"))
    }

    @Test
    fun amountRejectsInvalid() {
        assertNull(parseAmountToCents(""))
        assertNull(parseAmountToCents("abc"))
        assertNull(parseAmountToCents("1,000.00"))
        assertNull(parseAmountToCents("1.234"))
        assertNull(parseAmountToCents("."))
        assertNull(parseAmountToCents("-"))
    }

    @Test
    fun dateTimeRoundTrip() {
        val ldt = LocalDateTime(2026, 4, 23, 9, 30, 0, 0)
        val text = formatDateTime(ldt)
        assertEquals("2026-04-23 09:30", text)
        val millis = parseDateTime(text, UTC)!!
        val back = Instant.fromEpochMilliseconds(millis)
        assertEquals(ldt.toInstant(UTC), back)
    }

    @Test
    fun dateTimeRejectsMalformed() {
        assertNull(parseDateTime("2026/04/23 09:30", UTC))
        assertNull(parseDateTime("2026-04-23", UTC))
        assertNull(parseDateTime("2026-13-01 09:30", UTC))
        assertNull(parseDateTime("", UTC))
    }

    @Test
    fun formatCsvProducesHeaderAndRows() {
        val records = listOf(
            recordOf(
                RecordKind.Expense, 3500, "餐饮", "午餐", "招行储蓄", "食堂",
                LocalDateTime(2026, 4, 23, 9, 30, 0, 0),
            ),
            recordOf(
                RecordKind.Income, 1850000, "工资", null, "招行储蓄", "",
                LocalDateTime(2026, 4, 23, 18, 0, 0, 0),
            ),
        )
        val csv = formatCsv(records, UTC)
        val lines = csv.trimEnd('\n').split("\n")
        assertEquals(3, lines.size)
        assertEquals("日期,类型,大类,小类,金额,账户,备注", lines[0])
        assertEquals("2026-04-23 09:30,支出,餐饮,午餐,35.00,招行储蓄,食堂", lines[1])
        assertEquals("2026-04-23 18:00,收入,工资,,18500.00,招行储蓄,", lines[2])
    }

    @Test
    fun formatCsvEscapesTrickyNote() {
        val records = listOf(
            recordOf(
                RecordKind.Expense, 4250, "交通", "出租车", "微信",
                "打的到机场，附赠备注\"测试\"",
                LocalDateTime(2026, 4, 20, 12, 0, 0, 0),
            ),
        )
        val csv = formatCsv(records, UTC)
        val lines = csv.trimEnd('\n').split("\n")
        // note 里有 " 要转义、fields 里没有英文逗号所以只有 note 和 出租车 都不需要带引号
        // 打的到机场 带中文逗号不是英文逗号，不需要加引号
        assertTrue(lines[1].endsWith(",\"打的到机场，附赠备注\"\"测试\"\"\""))
    }

    @Test
    fun roundTripFormatAndParse() {
        val records = listOf(
            recordOf(
                RecordKind.Expense, 3500, "餐饮", "午餐", "招行储蓄", "食堂",
                LocalDateTime(2026, 4, 23, 9, 30, 0, 0),
            ),
            recordOf(
                RecordKind.Income, 1850000, "工资", null, "招行储蓄", "",
                LocalDateTime(2026, 4, 23, 18, 0, 0, 0),
            ),
            recordOf(
                RecordKind.Expense, 4250, "交通", "出租车", "微信",
                "打的，含逗号和 \"引号\" 还有\n换行",
                LocalDateTime(2026, 4, 20, 12, 0, 0, 0),
            ),
        )
        val csv = formatCsv(records, UTC)
        val parsed = parseCsv(csv)
        assertEquals(3, parsed.size)
        assertEquals("2026-04-23 09:30", parsed[0].dateText)
        assertEquals("支出", parsed[0].typeText)
        assertEquals("餐饮", parsed[0].categoryName)
        assertEquals("午餐", parsed[0].subCategoryName)
        assertEquals("35.00", parsed[0].amountText)
        assertEquals("招行储蓄", parsed[0].accountName)
        assertEquals("食堂", parsed[0].note)

        assertEquals("", parsed[1].subCategoryName)
        assertEquals("", parsed[1].note)

        assertEquals("打的，含逗号和 \"引号\" 还有\n换行", parsed[2].note)
    }

    @Test
    fun parseCsvEmptyReturnsEmpty() {
        assertEquals(emptyList(), parseCsv(""))
    }

    @Test
    fun parseCsvOnlyHeaderReturnsEmpty() {
        assertEquals(emptyList(), parseCsv("日期,类型,大类,小类,金额,账户,备注\n"))
    }
}
