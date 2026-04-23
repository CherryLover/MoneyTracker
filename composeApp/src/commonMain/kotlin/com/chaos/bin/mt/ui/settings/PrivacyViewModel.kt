package com.chaos.bin.mt.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaos.bin.mt.data.Category
import com.chaos.bin.mt.data.RecordKind
import com.chaos.bin.mt.di.AppContainer
import com.chaos.bin.mt.ui.home.PrefKeys
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 一条已标记隐私的条目（按大类 or 小类）。 */
data class PrivacyEntry(
    val categoryId: String,
    val categoryName: String,
    val categoryEmoji: String,
    val kind: RecordKind,
    /** null = 整个大类；非 null = 某个小类。 */
    val subCategoryId: String?,
    val subCategoryName: String?,
)

data class PrivacyUiState(
    val maskExpense: Boolean = false,
    val maskIncome: Boolean = false,
    val maskBalance: Boolean = false,
    val entries: List<PrivacyEntry> = emptyList(),
    val expense: List<Category> = emptyList(),
    val income: List<Category> = emptyList(),
)

class PrivacyViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<PrivacyUiState> = combine(
        combine(
            container.preferenceRepository.observe(PrefKeys.PRIVACY_HOME_EXPENSE),
            container.preferenceRepository.observe(PrefKeys.PRIVACY_HOME_INCOME),
            container.preferenceRepository.observe(PrefKeys.PRIVACY_HOME_BALANCE),
        ) { e, i, b -> Triple(e == "1", i == "1", b == "1") },
        container.categoryRepository.observeTree(RecordKind.Expense),
        container.categoryRepository.observeTree(RecordKind.Income),
    ) { masks, expense, income ->
        PrivacyUiState(
            maskExpense = masks.first,
            maskIncome = masks.second,
            maskBalance = masks.third,
            entries = buildEntries(expense, RecordKind.Expense) + buildEntries(income, RecordKind.Income),
            expense = expense,
            income = income,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PrivacyUiState(),
    )

    private fun buildEntries(list: List<Category>, kind: RecordKind): List<PrivacyEntry> =
        buildList {
            list.forEach { cat ->
                if (cat.privacy) {
                    add(
                        PrivacyEntry(
                            categoryId = cat.id,
                            categoryName = cat.name,
                            categoryEmoji = cat.emoji,
                            kind = kind,
                            subCategoryId = null,
                            subCategoryName = null,
                        ),
                    )
                }
                cat.subs.forEach { sub ->
                    if (sub.privacy) {
                        add(
                            PrivacyEntry(
                                categoryId = cat.id,
                                categoryName = cat.name,
                                categoryEmoji = cat.emoji,
                                kind = kind,
                                subCategoryId = sub.id,
                                subCategoryName = sub.name,
                            ),
                        )
                    }
                }
            }
        }

    fun setMaskExpense(on: Boolean) = viewModelScope.launch {
        container.preferenceRepository.setBool(PrefKeys.PRIVACY_HOME_EXPENSE, on)
    }

    fun setMaskIncome(on: Boolean) = viewModelScope.launch {
        container.preferenceRepository.setBool(PrefKeys.PRIVACY_HOME_INCOME, on)
    }

    fun setMaskBalance(on: Boolean) = viewModelScope.launch {
        container.preferenceRepository.setBool(PrefKeys.PRIVACY_HOME_BALANCE, on)
    }

    fun addCategory(categoryId: String) = viewModelScope.launch {
        container.categoryRepository.setCategoryPrivacy(categoryId, true)
    }

    fun addSub(subCategoryId: String) = viewModelScope.launch {
        container.categoryRepository.setSubCategoryPrivacy(subCategoryId, true)
    }

    fun remove(entry: PrivacyEntry) = viewModelScope.launch {
        if (entry.subCategoryId == null) {
            container.categoryRepository.setCategoryPrivacy(entry.categoryId, false)
        } else {
            container.categoryRepository.setSubCategoryPrivacy(entry.subCategoryId, false)
        }
    }
}
