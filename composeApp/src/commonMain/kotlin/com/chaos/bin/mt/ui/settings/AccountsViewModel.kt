package com.chaos.bin.mt.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaos.bin.mt.data.Account
import com.chaos.bin.mt.data.nowMillis
import com.chaos.bin.mt.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val defaultId: String? = null,
)

class AccountsViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<AccountsUiState> = combine(
        container.accountRepository.observeAll(),
        container.preferenceRepository.observe("default_account_id"),
    ) { accounts, defaultId ->
        AccountsUiState(accounts = accounts, defaultId = defaultId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountsUiState(),
    )

    fun add(name: String, emoji: String) = viewModelScope.launch {
        val id = "account-${nowMillis()}"
        val isFirstOrNoDefault = state.value.accounts.isEmpty() || state.value.defaultId == null
        val sortIndex = state.value.accounts.size.toLong()
        container.accountRepository.insert(
            id = id,
            name = name.trim().ifEmpty { "新账户" },
            emoji = emoji.trim().ifEmpty { "💳" },
            sortIndex = sortIndex,
        )
        if (isFirstOrNoDefault) {
            container.preferenceRepository.set("default_account_id", id)
        }
    }

    fun update(id: String, name: String, emoji: String) = viewModelScope.launch {
        container.accountRepository.update(
            id = id,
            name = name.trim().ifEmpty { "新账户" },
            emoji = emoji.trim().ifEmpty { "💳" },
        )
    }

    fun setDefault(id: String) = viewModelScope.launch {
        container.preferenceRepository.set("default_account_id", id)
    }

    fun delete(id: String) = viewModelScope.launch {
        val wasDefault = state.value.defaultId == id
        container.accountRepository.delete(id)
        if (wasDefault) {
            val remaining = state.value.accounts.filter { it.id != id }
            val next = remaining.firstOrNull()
            if (next != null) {
                container.preferenceRepository.set("default_account_id", next.id)
            } else {
                container.preferenceRepository.delete("default_account_id")
            }
        }
    }

    suspend fun hasRecords(id: String): Boolean =
        container.recordRepository.countByAccount(id) > 0L
}
