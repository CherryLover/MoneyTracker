package com.chaos.bin.mt.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.chaos.bin.mt.db.MtDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime

/** 大类 + 小类的组合操作。 */
class CategoryRepository(private val db: MtDatabase) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTree(kind: RecordKind): Flow<List<Category>> {
        val catsFlow = db.categoryQueries.selectByKind(kind.code.toLong())
            .asFlow()
            .mapToList(Dispatchers.Default)
        return catsFlow.flatMapLatest { rows ->
            if (rows.isEmpty()) {
                flowOf(emptyList())
            } else {
                val subFlows = rows.map { row ->
                    db.subCategoryQueries.selectByCategory(row.id)
                        .asFlow()
                        .mapToList(Dispatchers.Default)
                }
                combine(subFlows) { subsPerCat ->
                    rows.mapIndexed { i, row ->
                        Category(
                            id = row.id,
                            name = row.name,
                            emoji = row.emoji,
                            privacy = row.privacy != 0L,
                            subs = subsPerCat[i].map {
                                SubCategory(id = it.id, name = it.name, privacy = it.privacy != 0L)
                            },
                        )
                    }
                }
            }
        }
    }

    suspend fun count(): Long = withContext(Dispatchers.Default) {
        db.categoryQueries.countAll().executeAsOne()
    }

    suspend fun insertCategory(
        id: String,
        name: String,
        emoji: String,
        kind: RecordKind,
        privacy: Boolean,
        sortIndex: Long,
    ) = withContext(Dispatchers.Default) {
        db.categoryQueries.insert(
            id = id,
            name = name,
            emoji = emoji,
            kind = kind.code.toLong(),
            privacy = if (privacy) 1 else 0,
            sort_index = sortIndex,
            created_at = nowMillis(),
        )
    }

    suspend fun insertSubCategory(
        id: String,
        categoryId: String,
        name: String,
        privacy: Boolean,
        sortIndex: Long,
    ) = withContext(Dispatchers.Default) {
        db.subCategoryQueries.insert(
            id = id,
            category_id = categoryId,
            name = name,
            privacy = if (privacy) 1 else 0,
            sort_index = sortIndex,
            created_at = nowMillis(),
        )
    }
}

class AccountRepository(private val db: MtDatabase) {

    fun observeAll(): Flow<List<Account>> =
        db.accountQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { Account(id = it.id, name = it.name, emoji = it.emoji) } }

    suspend fun count(): Long = withContext(Dispatchers.Default) {
        db.accountQueries.countAll().executeAsOne()
    }

    suspend fun insert(
        id: String,
        name: String,
        emoji: String,
        sortIndex: Long,
    ) = withContext(Dispatchers.Default) {
        db.accountQueries.insert(
            id = id,
            name = name,
            emoji = emoji,
            sort_index = sortIndex,
            created_at = nowMillis(),
        )
    }
}

class RecordRepository(private val db: MtDatabase) {

    fun observeMonth(year: Int, month: Int): Flow<List<RecordDetail>> {
        val (start, end) = monthRangeMillis(year, month)
        return db.recordQueries.selectInRangeDetailed(start, end - 1)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }
    }

    /** 每年每月的支出 / 收入汇总，按本地时区聚合。 */
    fun observeMonthlySummaries(): Flow<Map<Pair<Int, Int>, MonthSummary>> {
        return db.recordQueries.selectAllStatsFields()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                val acc = HashMap<Pair<Int, Int>, LongArray>()
                for (r in rows) {
                    val ldt = Instant.fromEpochMilliseconds(r.occurred_at)
                        .toLocalDateTime(AppTimeZone)
                    val key = ldt.year to ldt.monthNumber
                    val bucket = acc.getOrPut(key) { LongArray(2) }
                    if (r.kind == 0L) bucket[0] += r.amount_cents
                    else bucket[1] += r.amount_cents
                }
                acc.mapValues { (_, v) -> MonthSummary(expenseCents = v[0], incomeCents = v[1]) }
            }
    }

    fun observeMonthSummary(year: Int, month: Int): Flow<MonthSummary> {
        val (start, end) = monthRangeMillis(year, month)
        return db.recordQueries.sumByKindInRange(start, end - 1)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                var expense = 0L
                var income = 0L
                for (r in rows) {
                    when (r.kind) {
                        0L -> expense = r.total_cents ?: 0L
                        1L -> income = r.total_cents ?: 0L
                    }
                }
                MonthSummary(expense, income)
            }
    }

    suspend fun insert(
        kind: RecordKind,
        amountCents: Long,
        categoryId: String,
        subCategoryId: String?,
        accountId: String,
        note: String,
        occurredAt: Instant,
        privacy: Boolean,
        source: Int = 0,
        autoRuleId: Long? = null,
    ): Long = withContext(Dispatchers.Default) {
        val now = nowMillis()
        db.recordQueries.insert(
            kind = kind.code.toLong(),
            amount_cents = amountCents,
            category_id = categoryId,
            sub_category_id = subCategoryId,
            account_id = accountId,
            note = note,
            occurred_at = occurredAt.toEpochMilliseconds(),
            privacy = if (privacy) 1 else 0,
            source = source.toLong(),
            auto_rule_id = autoRuleId,
            created_at = now,
            updated_at = now,
        )
        db.recordQueries.lastInsertRowId().executeAsOne()
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.Default) {
        db.recordQueries.deleteById(id)
    }

    suspend fun getRawById(id: Long): RawRecord? = withContext(Dispatchers.Default) {
        val row = db.recordQueries.selectById(id).executeAsOneOrNull() ?: return@withContext null
        RawRecord(
            id = row.id,
            kind = RecordKind.fromCode(row.kind.toInt()),
            amountCents = row.amount_cents,
            categoryId = row.category_id,
            subCategoryId = row.sub_category_id,
            accountId = row.account_id,
            note = row.note,
            occurredAt = Instant.fromEpochMilliseconds(row.occurred_at),
            privacy = row.privacy != 0L,
        )
    }

    suspend fun update(
        id: Long,
        kind: RecordKind,
        amountCents: Long,
        categoryId: String,
        subCategoryId: String?,
        accountId: String,
        note: String,
        occurredAt: Instant,
        privacy: Boolean,
    ) = withContext(Dispatchers.Default) {
        db.recordQueries.update(
            kind = kind.code.toLong(),
            amount_cents = amountCents,
            category_id = categoryId,
            sub_category_id = subCategoryId,
            account_id = accountId,
            note = note,
            occurred_at = occurredAt.toEpochMilliseconds(),
            privacy = if (privacy) 1 else 0,
            updated_at = nowMillis(),
            id = id,
        )
    }
}

class PreferenceRepository(private val db: MtDatabase) {

    fun observe(key: String): Flow<String?> =
        db.preferenceQueries.get(key)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)

    suspend fun get(key: String): String? = withContext(Dispatchers.Default) {
        db.preferenceQueries.get(key).executeAsOneOrNull()
    }

    suspend fun set(key: String, value: String) = withContext(Dispatchers.Default) {
        db.preferenceQueries.set(key, value)
    }

    suspend fun setBool(key: String, value: Boolean) = set(key, if (value) "1" else "0")

    suspend fun getBool(key: String, default: Boolean = false): Boolean =
        get(key)?.let { it == "1" } ?: default
}

/** SQLDelight 生成的投影行 → 领域类型。 */
private fun com.chaos.bin.mt.db.SelectInRangeDetailed.toDomain(): RecordDetail = RecordDetail(
    id = id,
    kind = RecordKind.fromCode(kind.toInt()),
    amountCents = amount_cents,
    categoryId = category_id,
    subCategoryId = sub_category_id,
    accountId = account_id,
    categoryName = category_name,
    categoryEmoji = category_emoji,
    categoryPrivacy = category_privacy != 0L,
    subCategoryName = sub_category_name,
    subCategoryPrivacy = (sub_category_privacy ?: 0L) != 0L,
    accountName = account_name,
    accountEmoji = account_emoji,
    note = note,
    occurredAt = Instant.fromEpochMilliseconds(occurred_at),
    privacy = privacy != 0L,
)
