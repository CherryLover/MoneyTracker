package com.chaos.bin.mt

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.chaos.bin.mt.data.CategoryRepository
import com.chaos.bin.mt.data.RecordKind
import com.chaos.bin.mt.data.RecordRepository
import com.chaos.bin.mt.db.MtDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryRepositoryTest {

    private fun newDb(): MtDatabase {
        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite::memory:",
            properties = Properties(),
        )
        MtDatabase.Schema.create(driver)
        return MtDatabase(driver)
    }

    @Test
    fun categoryCrudAndCascadeSubs() = runBlocking {
        val db = newDb()
        val repo = CategoryRepository(db)
        repo.insertCategory("cat1", "餐饮", "🍚", RecordKind.Expense, false, 0)
        repo.insertSubCategory("sub1", "cat1", "早餐", false, 0)
        repo.insertSubCategory("sub2", "cat1", "午餐", true, 1)

        repo.updateCategory("cat1", "吃喝", "🍜", true)
        val row = db.categoryQueries.selectById("cat1").executeAsOne()
        assertEquals("吃喝", row.name)
        assertEquals("🍜", row.emoji)
        assertEquals(1L, row.privacy)

        repo.updateSubCategory("sub1", "早点", true)
        val s1 = db.subCategoryQueries.selectById("sub1").executeAsOne()
        assertEquals("早点", s1.name)
        assertEquals(1L, s1.privacy)

        repo.updateCategorySortIndex("cat1", 5)
        assertEquals(5L, db.categoryQueries.selectById("cat1").executeAsOne().sort_index)

        // 删除大类：其下的小类应当一起清掉（手动级联）
        repo.deleteCategory("cat1")
        assertEquals(0L, db.categoryQueries.countAll().executeAsOne())
        val remainingSubs = db.subCategoryQueries.selectByCategory("cat1").executeAsList()
        assertTrue(remainingSubs.isEmpty())
    }

    @Test
    fun recordCountByCategoryAndSub() = runBlocking {
        val db = newDb()
        val cat = CategoryRepository(db)
        val rec = RecordRepository(db)

        db.accountQueries.insert(
            id = "acc1",
            name = "现金",
            emoji = "💵",
            sort_index = 0,
            created_at = 0,
        )
        cat.insertCategory("cat1", "餐饮", "🍚", RecordKind.Expense, false, 0)
        cat.insertSubCategory("sub1", "cat1", "早餐", false, 0)

        rec.insert(
            kind = RecordKind.Expense,
            amountCents = 1000,
            categoryId = "cat1",
            subCategoryId = "sub1",
            accountId = "acc1",
            note = "",
            occurredAt = Instant.fromEpochMilliseconds(1_000),
            privacy = false,
        )
        rec.insert(
            kind = RecordKind.Expense,
            amountCents = 500,
            categoryId = "cat1",
            subCategoryId = null,
            accountId = "acc1",
            note = "",
            occurredAt = Instant.fromEpochMilliseconds(1_000),
            privacy = false,
        )

        assertEquals(2L, rec.countByCategory("cat1"))
        assertEquals(1L, rec.countBySubCategory("sub1"))
    }
}
