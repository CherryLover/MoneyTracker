package com.chaos.bin.mt.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaos.bin.mt.data.Category
import com.chaos.bin.mt.data.RecordKind
import com.chaos.bin.mt.data.nowMillis
import com.chaos.bin.mt.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val kind: RecordKind = RecordKind.Expense,
    val expense: List<Category> = emptyList(),
    val income: List<Category> = emptyList(),
    val selectedExpenseId: String? = null,
    val selectedIncomeId: String? = null,
) {
    val currentList: List<Category>
        get() = if (kind == RecordKind.Expense) expense else income

    val selectedId: String?
        get() = if (kind == RecordKind.Expense) selectedExpenseId else selectedIncomeId

    val currentCategory: Category?
        get() = currentList.firstOrNull { it.id == selectedId } ?: currentList.firstOrNull()
}

class CategoriesViewModel(private val container: AppContainer) : ViewModel() {

    private val tab = MutableStateFlow(RecordKind.Expense)
    private val selectedExpense = MutableStateFlow<String?>(null)
    private val selectedIncome = MutableStateFlow<String?>(null)

    val state: StateFlow<CategoriesUiState> = combine(
        container.categoryRepository.observeTree(RecordKind.Expense),
        container.categoryRepository.observeTree(RecordKind.Income),
        tab,
        selectedExpense,
        selectedIncome,
    ) { expense, income, kind, selE, selI ->
        CategoriesUiState(
            kind = kind,
            expense = expense,
            income = income,
            selectedExpenseId = selE?.takeIf { id -> expense.any { it.id == id } } ?: expense.firstOrNull()?.id,
            selectedIncomeId = selI?.takeIf { id -> income.any { it.id == id } } ?: income.firstOrNull()?.id,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoriesUiState(),
    )

    fun switchKind(kind: RecordKind) {
        tab.value = kind
    }

    fun select(categoryId: String) {
        if (tab.value == RecordKind.Expense) selectedExpense.value = categoryId
        else selectedIncome.value = categoryId
    }

    // ——— 大类 ———
    fun addCategory(name: String, emoji: String) = viewModelScope.launch {
        val kind = tab.value
        val list = if (kind == RecordKind.Expense) state.value.expense else state.value.income
        val id = "cat-${nowMillis()}"
        container.categoryRepository.insertCategory(
            id = id,
            name = name.trim().ifEmpty { "新分类" },
            emoji = emoji.trim().ifEmpty { "🏷️" },
            kind = kind,
            privacy = false,
            sortIndex = list.size.toLong(),
        )
        if (kind == RecordKind.Expense) selectedExpense.value = id
        else selectedIncome.value = id
    }

    /** 分类管理只改名称 / emoji。隐私标记由「隐私保护」页集中管理，这里原样保留。 */
    fun updateCategory(id: String, name: String, emoji: String) = viewModelScope.launch {
        val existing = (state.value.expense + state.value.income).firstOrNull { it.id == id }
        container.categoryRepository.updateCategory(
            id = id,
            name = name.trim().ifEmpty { "新分类" },
            emoji = emoji.trim().ifEmpty { "🏷️" },
            privacy = existing?.privacy ?: false,
        )
    }

    fun deleteCategory(id: String) = viewModelScope.launch {
        container.categoryRepository.deleteCategory(id)
    }

    /** 按给定新顺序重写 sort_index（0..n-1），驱动 UI 重排。 */
    fun reorderCategories(orderedIds: List<String>) = viewModelScope.launch {
        orderedIds.forEachIndexed { i, id ->
            container.categoryRepository.updateCategorySortIndex(id, i.toLong())
        }
    }

    suspend fun hasRecordsForCategory(id: String): Boolean =
        container.recordRepository.countByCategory(id) > 0L

    // ——— 小类 ———
    fun addSub(categoryId: String, name: String) = viewModelScope.launch {
        val parent = state.value.currentList.firstOrNull { it.id == categoryId } ?: return@launch
        val id = "sub-${nowMillis()}"
        container.categoryRepository.insertSubCategory(
            id = id,
            categoryId = categoryId,
            name = name.trim().ifEmpty { "新小类" },
            privacy = false,
            sortIndex = parent.subs.size.toLong(),
        )
    }

    fun updateSub(id: String, name: String) = viewModelScope.launch {
        val existing = (state.value.expense + state.value.income)
            .flatMap { it.subs }.firstOrNull { it.id == id }
        container.categoryRepository.updateSubCategory(
            id = id,
            name = name.trim().ifEmpty { "新小类" },
            privacy = existing?.privacy ?: false,
        )
    }

    fun deleteSub(id: String) = viewModelScope.launch {
        container.categoryRepository.deleteSubCategory(id)
    }

    fun reorderSubs(orderedIds: List<String>) = viewModelScope.launch {
        orderedIds.forEachIndexed { i, id ->
            container.categoryRepository.updateSubCategorySortIndex(id, i.toLong())
        }
    }

    suspend fun hasRecordsForSub(id: String): Boolean =
        container.recordRepository.countBySubCategory(id) > 0L
}
