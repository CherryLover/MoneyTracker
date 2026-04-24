package com.chaos.bin.mt

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.chaos.bin.mt.data.AccountRepository
import com.chaos.bin.mt.data.CategoryRepository
import com.chaos.bin.mt.data.CsvImportService
import com.chaos.bin.mt.data.RecordKind
import com.chaos.bin.mt.data.RecordRepository
import com.chaos.bin.mt.db.MtDatabase
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone

class CsvImportServiceTest {

    private val zone = TimeZone.UTC

    private fun newDb(): MtDatabase {
        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite::memory:",
            properties = Properties(),
        )
        MtDatabase.Schema.create(driver)
        return MtDatabase(driver)
    }

    private fun seed(db: MtDatabase) = runBlocking {
        val cat = CategoryRepository(db)
        val acc = AccountRepository(db)
        cat.insertCategory("cat_food", "餐饮", "🍚", RecordKind.Expense, false, 0)
        cat.insertSubCategory("sub_lunch", "cat_food", "午餐", false, 0)
        cat.insertCategory("cat_salary", "工资", "💰", RecordKind.Income, false, 0)
        acc.insert("acc_card", "招行储蓄", "💳", 0)
    }

    private fun service(db: MtDatabase) = CsvImportService(
        categoryRepository = CategoryRepository(db),
        accountRepository = AccountRepository(db),
        recordRepository = RecordRepository(db),
    )

    @Test
    fun importInsertsValidRows() = runBlocking {
        val db = newDb()
        seed(db)
        val csv = """
            日期,类型,大类,小类,金额,账户,备注
            2026-04-23 09:30,支出,餐饮,午餐,35.00,招行储蓄,食堂
            2026-04-23 18:00,收入,工资,,18500.00,招行储蓄,
        """.trimIndent() + "\n"

        val result = service(db).importCsv(csv, zone)
        assertEquals(2, result.insertedCount)
        assertEquals(0, result.skippedCount)
        val stored = RecordRepository(db).getAll()
        assertEquals(2, stored.size)
        val expense = stored.first { it.kind == RecordKind.Expense }
        assertEquals(3500, expense.amountCents)
        assertEquals("餐饮", expense.categoryName)
        assertEquals("午餐", expense.subCategoryName)
        val income = stored.first { it.kind == RecordKind.Income }
        assertEquals(1850000, income.amountCents)
        assertEquals("工资", income.categoryName)
        assertEquals(null, income.subCategoryName)
    }

    @Test
    fun importSkipsUnknownCategorySubAccount() = runBlocking {
        val db = newDb()
        seed(db)
        val csv = """
            日期,类型,大类,小类,金额,账户,备注
            2026-04-23 09:30,支出,不存在,午餐,35.00,招行储蓄,
            2026-04-23 09:30,支出,餐饮,不存在小类,35.00,招行储蓄,
            2026-04-23 09:30,支出,餐饮,午餐,35.00,不存在账户,
        """.trimIndent() + "\n"

        val result = service(db).importCsv(csv, zone)
        assertEquals(0, result.insertedCount)
        assertEquals(3, result.skippedCount)
        assertEquals(3, result.errors.size)
        assertTrue(RecordRepository(db).getAll().isEmpty())
    }

    @Test
    fun importSkipsBadAmountAndDateAndType() = runBlocking {
        val db = newDb()
        seed(db)
        val csv = """
            日期,类型,大类,小类,金额,账户,备注
            bad-date,支出,餐饮,午餐,35.00,招行储蓄,
            2026-04-23 09:30,支出,餐饮,午餐,abc,招行储蓄,
            2026-04-23 09:30,未知,餐饮,午餐,35.00,招行储蓄,
        """.trimIndent() + "\n"

        val result = service(db).importCsv(csv, zone)
        assertEquals(0, result.insertedCount)
        assertEquals(3, result.skippedCount)
        assertTrue(RecordRepository(db).getAll().isEmpty())
    }

    @Test
    fun importEmptyContent() = runBlocking {
        val db = newDb()
        seed(db)
        val result = service(db).importCsv("", zone)
        assertEquals(0, result.insertedCount)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun importOnlyHeader() = runBlocking {
        val db = newDb()
        seed(db)
        val result = service(db).importCsv("日期,类型,大类,小类,金额,账户,备注\n", zone)
        assertEquals(0, result.insertedCount)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun importMixedResult() = runBlocking {
        val db = newDb()
        seed(db)
        val csv = """
            日期,类型,大类,小类,金额,账户,备注
            2026-04-23 09:30,支出,餐饮,午餐,35.00,招行储蓄,OK
            2026-04-23 10:00,支出,未知分类,午餐,10.00,招行储蓄,skip
            2026-04-23 11:00,收入,工资,,500.00,招行储蓄,OK
        """.trimIndent() + "\n"

        val result = service(db).importCsv(csv, zone)
        assertEquals(2, result.insertedCount)
        assertEquals(1, result.skippedCount)
        assertEquals(2, RecordRepository(db).getAll().size)
    }
}
