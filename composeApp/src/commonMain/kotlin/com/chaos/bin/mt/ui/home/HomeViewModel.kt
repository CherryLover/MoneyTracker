package com.chaos.bin.mt.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaos.bin.mt.data.AppTimeZone
import com.chaos.bin.mt.data.Category
import com.chaos.bin.mt.data.DayGroup
import com.chaos.bin.mt.data.MonthSummary
import com.chaos.bin.mt.data.RecordDetail
import com.chaos.bin.mt.data.RecordKind
import com.chaos.bin.mt.data.toLocalDate
import com.chaos.bin.mt.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.chaos.bin.mt.data.nowInstant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

data class HomeUiState(
    val year: Int,
    val month: Int,
    val dayGroups: List<DayGroup> = emptyList(),
    val summary: MonthSummary = MonthSummary(0, 0),
    val maskHomeExpense: Boolean = false,
    val maskHomeIncome: Boolean = false,
    val maskHomeBalance: Boolean = false,
    val hasAccounts: Boolean = true,
    /** 本月每天的支出汇总（分），下标 0 = 1 号，长度 = 当月天数。 */
    val dailyExpenseCents: List<Long> = emptyList(),
    /** 今天对应的下标（仅当选中月 == 当前月时 >=0）。 */
    val todayIndex: Int = -1,
    /** 今天所在年月（不随选中月变）。 */
    val todayYear: Int,
    val todayMonth: Int,
    /** 每年每月的全量汇总，供月份 picker 展示。 */
    val monthlySummaries: Map<Pair<Int, Int>, MonthSummary> = emptyMap(),
    /** 暂时被点击揭示的金额 key（5s 自动 re-mask）。 */
    val revealedKeys: Set<String> = emptySet(),
) {
    val isOnCurrentMonth: Boolean get() = year == todayYear && month == todayMonth
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val now = nowInstant().toLocalDateTime(AppTimeZone)
    private val monthSelection = MutableStateFlow(now.year to now.monthNumber)

    private val homeMasks = combine(
        container.preferenceRepository.observe(PrefKeys.PRIVACY_HOME_EXPENSE),
        container.preferenceRepository.observe(PrefKeys.PRIVACY_HOME_INCOME),
        container.preferenceRepository.observe(PrefKeys.PRIVACY_HOME_BALANCE),
    ) { e, i, b -> HomeMasks(e == "1", i == "1", b == "1") }

    private val revealedKeysFlow = MutableStateFlow<Set<String>>(emptySet())
    private val revealJobs = mutableMapOf<String, Job>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeUiState> = monthSelection
        .flatMapLatest { (y, m) ->
            val coreFlow = combine(
                liveMonthRecords(y, m),
                container.recordRepository.observeMonthSummary(y, m),
                homeMasks,
                container.accountRepository.observeAll(),
                container.recordRepository.observeMonthlySummaries(),
            ) { records, summary, masks, accounts, monthlies ->
                CoreSnapshot(records, summary, masks, accounts.isNotEmpty(), monthlies)
            }
            combine(coreFlow, revealedKeysFlow) { core, revealed ->
                val records = core.records
                val summary = core.summary
                val masks = core.masks
                val hasAccounts = core.hasAccounts
                val monthlies = core.monthlies
                val groups = groupByDay(records)
                val daysInMonth = daysInMonth(y, m)
                val daily = LongArray(daysInMonth)
                for (g in groups) {
                    val dayIdx = g.date.dayOfMonth - 1
                    if (dayIdx in daily.indices) daily[dayIdx] = g.expenseCentsSum
                }
                val todayIdx = if (y == now.year && m == now.monthNumber) now.dayOfMonth - 1 else -1
                HomeUiState(
                    year = y,
                    month = m,
                    dayGroups = groups,
                    summary = summary,
                    maskHomeExpense = masks.expense,
                    maskHomeIncome = masks.income,
                    maskHomeBalance = masks.balance,
                    hasAccounts = hasAccounts,
                    dailyExpenseCents = daily.toList(),
                    todayIndex = todayIdx,
                    todayYear = now.year,
                    todayMonth = now.monthNumber,
                    monthlySummaries = monthlies,
                    revealedKeys = revealed,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(
                year = now.year,
                month = now.monthNumber,
                todayYear = now.year,
                todayMonth = now.monthNumber,
            ),
        )

    /**
     * 把 month records 与 category trees combine，用最新 tree 覆盖每条 record 的 privacy 字段。
     * 这样大类/小类的 privacy 切换能立即反映到首页（observeMonth 的 JOIN 流不会随 Category 表变更而重新发出）。
     */
    private fun liveMonthRecords(y: Int, m: Int) = combine(
        container.recordRepository.observeMonth(y, m),
        container.categoryRepository.observeTree(RecordKind.Expense),
        container.categoryRepository.observeTree(RecordKind.Income),
    ) { records, expenseTree, incomeTree ->
        val byCatId: Map<String, Category> = (expenseTree + incomeTree).associateBy { it.id }
        records.map { rec ->
            val cat = byCatId[rec.categoryId]
            val newCatPrivacy = cat?.privacy ?: rec.categoryPrivacy
            val newSubPrivacy = rec.subCategoryId?.let { sid ->
                cat?.subs?.firstOrNull { it.id == sid }?.privacy
            } ?: rec.subCategoryPrivacy
            if (newCatPrivacy == rec.categoryPrivacy && newSubPrivacy == rec.subCategoryPrivacy) {
                rec
            } else {
                rec.copy(categoryPrivacy = newCatPrivacy, subCategoryPrivacy = newSubPrivacy)
            }
        }
    }

    fun selectMonth(year: Int, month: Int) {
        monthSelection.value = year to month
    }

    fun shiftMonth(delta: Int) {
        val (y, m) = monthSelection.value
        val total = y * 12 + (m - 1) + delta
        val ny = total / 12
        val nm = total % 12 + 1
        monthSelection.value = ny to nm
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            container.recordRepository.delete(id)
        }
    }

    /**
     * 揭示一个被遮蔽的金额 5 秒。如果 key 已在揭示集合内，no-op（保留原有 5s 截止时刻）。
     */
    fun reveal(key: String) {
        if (revealJobs[key]?.isActive == true) return
        revealedKeysFlow.update { it + key }
        revealJobs[key] = viewModelScope.launch {
            try {
                delay(5_000)
            } finally {
                revealedKeysFlow.update { it - key }
                revealJobs.remove(key)
            }
        }
    }

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
        else -> 30
    }

    private fun groupByDay(records: List<RecordDetail>): List<DayGroup> {
        if (records.isEmpty()) return emptyList()
        val byDate = linkedMapOf<LocalDate, MutableList<RecordDetail>>()
        for (r in records) {
            val d = r.occurredAt.toLocalDate()
            byDate.getOrPut(d) { mutableListOf() }.add(r)
        }
        return byDate.entries.sortedByDescending { it.key }
            .map { (date, items) -> DayGroup(date = date, items = items) }
    }
}

private data class HomeMasks(
    val expense: Boolean,
    val income: Boolean,
    val balance: Boolean,
)

private data class CoreSnapshot(
    val records: List<RecordDetail>,
    val summary: MonthSummary,
    val masks: HomeMasks,
    val hasAccounts: Boolean,
    val monthlies: Map<Pair<Int, Int>, MonthSummary>,
)

object PrefKeys {
    const val PRIVACY_HOME_EXPENSE = "privacy_home_expense"
    const val PRIVACY_HOME_INCOME = "privacy_home_income"
    const val PRIVACY_HOME_BALANCE = "privacy_home_balance"
    const val THEME_MODE = "theme_mode" // "system" | "light" | "dark"
}
