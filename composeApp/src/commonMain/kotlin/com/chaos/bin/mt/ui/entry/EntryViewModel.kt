package com.chaos.bin.mt.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaos.bin.mt.data.Account
import com.chaos.bin.mt.data.AppTimeZone
import com.chaos.bin.mt.data.Category
import com.chaos.bin.mt.data.RecordKind
import com.chaos.bin.mt.data.SubCategory
import com.chaos.bin.mt.data.nowInstant
import com.chaos.bin.mt.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

/** 用户在记一笔界面里正在编辑的草稿。 */
data class EntryDraft(
    val kind: RecordKind = RecordKind.Expense,
    val amountInput: String = "0",
    val selectedCategoryId: String? = null,
    val selectedSubCategoryId: String? = null,
    val selectedAccountId: String? = null,
    val note: String = "",
    val occurredAt: Instant? = null,
    /** 非 null 表示正在编辑现有记录，保存时走 update。 */
    val editingId: Long? = null,
)

data class EntryUiState(
    val draft: EntryDraft = EntryDraft(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
) {
    val kind: RecordKind get() = draft.kind
    val amountInput: String get() = draft.amountInput
    val selectedCategoryId: String? get() = draft.selectedCategoryId
    val selectedSubCategoryId: String? get() = draft.selectedSubCategoryId
    val selectedAccountId: String? get() = draft.selectedAccountId
    val note: String get() = draft.note
    val occurredAt: Instant? get() = draft.occurredAt
    val hasAccounts: Boolean get() = accounts.isNotEmpty()
    val currentCategory: Category?
        get() = categories.firstOrNull { it.id == selectedCategoryId } ?: categories.firstOrNull()
    val currentSubCategory: SubCategory?
        get() = currentCategory?.subs?.firstOrNull { it.id == selectedSubCategoryId }
            ?: currentCategory?.subs?.firstOrNull()
    val amountCents: Long? get() = parseAmountToCents(amountInput)
    val isEditing: Boolean get() = draft.editingId != null
}

class EntryViewModel(private val container: AppContainer) : ViewModel() {

    private val draftFlow = MutableStateFlow(EntryDraft())

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<EntryUiState> = combine(
        draftFlow,
        draftFlow.flatMapLatest { container.categoryRepository.observeTree(it.kind) },
        container.accountRepository.observeAll(),
    ) { draft, cats, accts ->
        // 若选中项不在新数据里，回退到首项
        val catId = draft.selectedCategoryId?.takeIf { id -> cats.any { it.id == id } }
            ?: cats.firstOrNull()?.id
        val currentCat = cats.firstOrNull { it.id == catId }
        val subId = draft.selectedSubCategoryId?.takeIf { id ->
            currentCat?.subs?.any { it.id == id } == true
        } ?: currentCat?.subs?.firstOrNull()?.id
        val accId = draft.selectedAccountId?.takeIf { id -> accts.any { it.id == id } }
            ?: accts.firstOrNull()?.id
        EntryUiState(
            draft = draft.copy(
                selectedCategoryId = catId,
                selectedSubCategoryId = subId,
                selectedAccountId = accId,
            ),
            categories = cats,
            accounts = accts,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EntryUiState(),
    )

    fun setKind(kind: RecordKind) {
        draftFlow.update { it.copy(kind = kind, selectedCategoryId = null, selectedSubCategoryId = null) }
    }

    fun selectCategory(id: String) {
        draftFlow.update { it.copy(selectedCategoryId = id, selectedSubCategoryId = null) }
    }

    fun selectSub(id: String) {
        draftFlow.update { it.copy(selectedSubCategoryId = id) }
    }

    fun selectAccount(id: String) {
        draftFlow.update { it.copy(selectedAccountId = id) }
    }

    fun setNote(text: String) {
        draftFlow.update { it.copy(note = text) }
    }

    fun setOccurredAt(at: Instant) {
        draftFlow.update { it.copy(occurredAt = at) }
    }

    fun typeDigit(c: Char) {
        draftFlow.update { it.copy(amountInput = appendDigit(it.amountInput, c)) }
    }

    fun typeDot() {
        draftFlow.update { it.copy(amountInput = appendDot(it.amountInput)) }
    }

    fun backspace() {
        draftFlow.update { it.copy(amountInput = backspaceOf(it.amountInput)) }
    }

    fun save(onSaved: ((isEdit: Boolean) -> Unit)? = null) {
        val s = state.value
        val cents = s.amountCents ?: return
        if (cents <= 0L) return
        val catId = s.selectedCategoryId ?: return
        val accId = s.selectedAccountId ?: return
        val occurredAt = s.occurredAt ?: nowInstant()
        val privacy = s.currentCategory?.privacy == true ||
            s.currentSubCategory?.privacy == true
        val editingId = s.draft.editingId
        viewModelScope.launch {
            if (editingId != null) {
                container.recordRepository.update(
                    id = editingId,
                    kind = s.kind,
                    amountCents = cents,
                    categoryId = catId,
                    subCategoryId = s.selectedSubCategoryId,
                    accountId = accId,
                    note = s.note.trim(),
                    occurredAt = occurredAt,
                    privacy = privacy,
                )
            } else {
                container.recordRepository.insert(
                    kind = s.kind,
                    amountCents = cents,
                    categoryId = catId,
                    subCategoryId = s.selectedSubCategoryId,
                    accountId = accId,
                    note = s.note.trim(),
                    occurredAt = occurredAt,
                    privacy = privacy,
                )
            }
            draftFlow.update { EntryDraft(kind = s.kind) }
            onSaved?.invoke(editingId != null)
        }
    }

    /** 从数据库读出一条记录，填进 draft 进入编辑模式。 */
    fun loadForEdit(recordId: Long) {
        viewModelScope.launch {
            val raw = container.recordRepository.getRawById(recordId) ?: return@launch
            // 金额分 -> yuan 字符串
            val yuanInt = raw.amountCents / 100
            val cents = (raw.amountCents % 100).toInt()
            val amountStr = if (cents == 0) yuanInt.toString() else {
                val centStr = cents.toString().padStart(2, '0').trimEnd('0')
                if (centStr.isEmpty()) yuanInt.toString() else "$yuanInt.$centStr"
            }
            draftFlow.update {
                EntryDraft(
                    kind = raw.kind,
                    amountInput = amountStr,
                    selectedCategoryId = raw.categoryId,
                    selectedSubCategoryId = raw.subCategoryId,
                    selectedAccountId = raw.accountId,
                    note = raw.note,
                    occurredAt = raw.occurredAt,
                    editingId = raw.id,
                )
            }
        }
    }

    fun cancelEditing() {
        draftFlow.update { EntryDraft() }
    }
}

internal fun parseAmountToCents(input: String): Long? {
    val s = input.trim()
    if (s.isEmpty() || s == ".") return null
    return runCatching {
        val d = s.toDouble()
        if (d < 0) null else kotlin.math.round(d * 100).toLong()
    }.getOrNull()
}

internal fun appendDigit(cur: String, c: Char): String {
    require(c in '0'..'9')
    val s = if (cur == "0") "" else cur
    val dot = s.indexOf('.')
    if (dot >= 0 && s.length - dot > 2) return cur
    val next = s + c
    return if (next.isEmpty()) "0" else next
}

internal fun appendDot(cur: String): String {
    if (cur.contains('.')) return cur
    return if (cur.isEmpty()) "0." else "$cur."
}

internal fun backspaceOf(cur: String): String {
    if (cur.length <= 1) return "0"
    return cur.dropLast(1)
}
