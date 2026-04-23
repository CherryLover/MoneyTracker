package com.chaos.bin.mt.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaos.bin.mt.data.Account
import com.chaos.bin.mt.data.AutoRule
import com.chaos.bin.mt.data.Category
import com.chaos.bin.mt.data.RecordKind
import com.chaos.bin.mt.data.TriggerConfig
import com.chaos.bin.mt.data.nowMillis
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

enum class TriggerTab { Weekly, MonthlyDays, Interval }

/** 编辑态持有的草稿（UI 输入态）。 */
data class AutomationEditDraft(
    val id: Long? = null,
    val name: String = "",
    val kind: RecordKind = RecordKind.Expense,
    val amountInput: String = "0",
    val categoryId: String? = null,
    val subCategoryId: String? = null,
    val accountId: String? = null,
    val note: String = "",
    val triggerTab: TriggerTab = TriggerTab.MonthlyDays,
    val weekdaysMask: Int = 0,
    val monthDaysMask: Int = 0,
    val intervalDays: Int = 7,
    val hour: Int = 9,
    val minute: Int = 0,
    val enabled: Boolean = true,
    val lastFiredAt: Long? = null,
    val createdAt: Long = 0,
    val loaded: Boolean = false,
)

data class AutomationEditUiState(
    val draft: AutomationEditDraft = AutomationEditDraft(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
) {
    val isEditing: Boolean get() = draft.id != null
    val currentCategory: Category?
        get() = categories.firstOrNull { it.id == draft.categoryId }
    val amountCents: Long? get() = parseYuanToCents(draft.amountInput)

    val isValid: Boolean
        get() {
            if (draft.name.trim().isEmpty()) return false
            val cents = amountCents ?: return false
            if (cents <= 0) return false
            if (draft.categoryId == null) return false
            if (draft.accountId == null) return false
            return when (draft.triggerTab) {
                TriggerTab.Weekly -> draft.weekdaysMask != 0
                TriggerTab.MonthlyDays -> draft.monthDaysMask != 0
                TriggerTab.Interval -> draft.intervalDays in 1..365
            }
        }
}

class AutomationEditViewModel(
    private val container: AppContainer,
    private val ruleId: Long?,
) : ViewModel() {

    private val draftFlow = MutableStateFlow(AutomationEditDraft(id = ruleId))

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<AutomationEditUiState> = combine(
        draftFlow,
        draftFlow.flatMapLatest { container.categoryRepository.observeTree(it.kind) },
        container.accountRepository.observeAll(),
    ) { draft, cats, accts ->
        val catId = draft.categoryId?.takeIf { id -> cats.any { it.id == id } }
            ?: cats.firstOrNull()?.id
        val currentCat = cats.firstOrNull { it.id == catId }
        val subId = draft.subCategoryId?.takeIf { id ->
            currentCat?.subs?.any { it.id == id } == true
        } ?: currentCat?.subs?.firstOrNull()?.id
        val accId = draft.accountId?.takeIf { id -> accts.any { it.id == id } }
            ?: accts.firstOrNull()?.id
        AutomationEditUiState(
            draft = draft.copy(
                categoryId = catId,
                subCategoryId = subId,
                accountId = accId,
            ),
            categories = cats,
            accounts = accts,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AutomationEditUiState(draft = AutomationEditDraft(id = ruleId)),
    )

    init {
        if (ruleId != null) loadExisting(ruleId)
        else draftFlow.update { it.copy(loaded = true) }
    }

    private fun loadExisting(id: Long) {
        viewModelScope.launch {
            val rule = container.autoRuleRepository.getById(id) ?: run {
                draftFlow.update { it.copy(loaded = true) }
                return@launch
            }
            val tab: TriggerTab
            var weekdays = 0
            var monthDays = 0
            var intervalDays = 7
            when (val t = rule.trigger) {
                is TriggerConfig.Weekly -> { tab = TriggerTab.Weekly; weekdays = t.weekdaysMask }
                is TriggerConfig.MonthlyDays -> { tab = TriggerTab.MonthlyDays; monthDays = t.daysMask }
                is TriggerConfig.Interval -> { tab = TriggerTab.Interval; intervalDays = t.intervalDays }
            }
            draftFlow.value = AutomationEditDraft(
                id = rule.id,
                name = rule.name,
                kind = rule.kind,
                amountInput = centsToYuanInput(rule.amountCents),
                categoryId = rule.categoryId,
                subCategoryId = rule.subCategoryId,
                accountId = rule.accountId,
                note = rule.note,
                triggerTab = tab,
                weekdaysMask = weekdays,
                monthDaysMask = monthDays,
                intervalDays = intervalDays,
                hour = rule.trigger.hour,
                minute = rule.trigger.minute,
                enabled = rule.enabled,
                lastFiredAt = rule.lastFiredAt,
                createdAt = rule.createdAt,
                loaded = true,
            )
        }
    }

    fun setName(v: String) = draftFlow.update { it.copy(name = v) }
    fun setKind(v: RecordKind) = draftFlow.update {
        it.copy(kind = v, categoryId = null, subCategoryId = null)
    }
    fun setAmountInput(v: String) = draftFlow.update { it.copy(amountInput = sanitizeAmountInput(v)) }
    fun setCategory(id: String) = draftFlow.update { it.copy(categoryId = id, subCategoryId = null) }
    fun setSub(id: String?) = draftFlow.update { it.copy(subCategoryId = id) }
    fun setAccount(id: String) = draftFlow.update { it.copy(accountId = id) }
    fun setNote(v: String) = draftFlow.update { it.copy(note = v) }
    fun setTriggerTab(tab: TriggerTab) = draftFlow.update { it.copy(triggerTab = tab) }
    fun toggleWeekday(bit: Int) = draftFlow.update {
        it.copy(weekdaysMask = it.weekdaysMask xor (1 shl bit))
    }
    fun toggleMonthDay(bit: Int) = draftFlow.update {
        it.copy(monthDaysMask = it.monthDaysMask xor (1 shl bit))
    }
    fun setIntervalDays(n: Int) = draftFlow.update {
        it.copy(intervalDays = n.coerceIn(1, 365))
    }
    fun setHour(v: Int) = draftFlow.update { it.copy(hour = v.coerceIn(0, 23)) }
    fun setMinute(v: Int) = draftFlow.update { it.copy(minute = v.coerceIn(0, 59)) }

    fun save(onDone: () -> Unit) {
        val s = state.value
        if (!s.isValid) return
        val d = s.draft
        val cents = s.amountCents ?: return
        val trigger: TriggerConfig = when (d.triggerTab) {
            TriggerTab.Weekly -> TriggerConfig.Weekly(d.weekdaysMask, d.hour, d.minute)
            TriggerTab.MonthlyDays -> TriggerConfig.MonthlyDays(d.monthDaysMask, d.hour, d.minute)
            TriggerTab.Interval -> TriggerConfig.Interval(d.intervalDays, d.hour, d.minute)
        }
        val now = nowMillis()
        viewModelScope.launch {
            if (d.id == null) {
                container.autoRuleRepository.insert(
                    AutoRule(
                        id = 0L,
                        name = d.name.trim(),
                        kind = d.kind,
                        amountCents = cents,
                        categoryId = d.categoryId!!,
                        subCategoryId = d.subCategoryId,
                        accountId = d.accountId!!,
                        note = d.note.trim(),
                        trigger = trigger,
                        enabled = d.enabled,
                        lastFiredAt = now,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            } else {
                container.autoRuleRepository.update(
                    AutoRule(
                        id = d.id,
                        name = d.name.trim(),
                        kind = d.kind,
                        amountCents = cents,
                        categoryId = d.categoryId!!,
                        subCategoryId = d.subCategoryId,
                        accountId = d.accountId!!,
                        note = d.note.trim(),
                        trigger = trigger,
                        enabled = d.enabled,
                        lastFiredAt = d.lastFiredAt,
                        createdAt = d.createdAt,
                        updatedAt = now,
                    )
                )
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = draftFlow.value.id ?: return
        viewModelScope.launch {
            container.autoRuleRepository.delete(id)
            onDone()
        }
    }
}

internal fun parseYuanToCents(input: String): Long? {
    val s = input.trim()
    if (s.isEmpty() || s == ".") return null
    return runCatching {
        val d = s.toDouble()
        if (d < 0) null else kotlin.math.round(d * 100).toLong()
    }.getOrNull()
}

internal fun sanitizeAmountInput(raw: String): String {
    // 只保留数字和第一个点
    val sb = StringBuilder()
    var dotSeen = false
    for (ch in raw) {
        when {
            ch.isDigit() -> sb.append(ch)
            ch == '.' && !dotSeen -> {
                dotSeen = true
                sb.append(ch)
            }
        }
    }
    var out = sb.toString()
    // 去掉前导 0（除 "0." 这种场景）
    if (out.length > 1 && out[0] == '0' && out[1] != '.') {
        out = out.trimStart('0')
        if (out.isEmpty() || out[0] == '.') out = "0$out"
    }
    if (out.isEmpty()) out = "0"
    // 限制小数位两位
    val idx = out.indexOf('.')
    if (idx >= 0 && out.length - idx - 1 > 2) {
        out = out.substring(0, idx + 3)
    }
    return out
}

internal fun centsToYuanInput(cents: Long): String {
    val yuan = cents / 100
    val c = (cents % 100).toInt()
    return if (c == 0) yuan.toString() else {
        val centStr = c.toString().padStart(2, '0').trimEnd('0')
        if (centStr.isEmpty()) yuan.toString() else "$yuan.$centStr"
    }
}
