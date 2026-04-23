package com.chaos.bin.mt.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaos.bin.mt.data.AutoRule
import com.chaos.bin.mt.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AutomationListViewModel(private val container: AppContainer) : ViewModel() {

    val rules: StateFlow<List<AutoRule>> = container.autoRuleRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun setEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        container.autoRuleRepository.updateEnabled(id, enabled)
    }
}
