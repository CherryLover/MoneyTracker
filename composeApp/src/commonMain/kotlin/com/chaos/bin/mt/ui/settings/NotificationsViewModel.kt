package com.chaos.bin.mt.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaos.bin.mt.data.ReminderRules
import com.chaos.bin.mt.data.ReminderSchedule
import com.chaos.bin.mt.data.formatReminderTime
import com.chaos.bin.mt.data.isReminderSupportedOnPlatform
import com.chaos.bin.mt.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val schedules: List<ReminderSchedule> = emptyList(),
    val permissionGranted: Boolean = true,
)

class NotificationsViewModel(private val container: AppContainer) : ViewModel() {
    private val permissionGranted = MutableStateFlow(true)

    val state: StateFlow<NotificationsUiState> = combine(
        container.reminderRepository.observe(),
        permissionGranted,
    ) { schedules, granted ->
        NotificationsUiState(schedules = schedules, permissionGranted = granted)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationsUiState(),
    )

    suspend fun refreshPermission(): Boolean {
        val granted = if (isReminderSupportedOnPlatform) container.notificationPermission.isGranted() else true
        permissionGranted.value = granted
        return granted
    }

    fun requestPermission() = viewModelScope.launch {
        permissionGranted.value = container.notificationPermission.request()
    }

    fun openAppSettings() {
        container.notificationPermission.openAppSettings()
    }

    fun validationError(hour: Int, minute: Int, editingId: Long?): String? {
        val schedules = state.value.schedules
        if (!ReminderRules.isValidTime(hour, minute)) return "提醒时间非法"
        if (editingId == null && schedules.size >= ReminderRules.MAX_COUNT) {
            return "最多只能配置 ${ReminderRules.MAX_COUNT} 条提醒"
        }
        val conflict = ReminderRules.findConflict(
            target = hour to minute,
            existing = schedules,
            excludingId = editingId,
        )
        return if (conflict != null) {
            "与 ${formatReminderTime(conflict)} 太近，至少间隔 ${ReminderRules.MIN_GAP_MINUTES} 分钟"
        } else {
            null
        }
    }

    fun save(editingId: Long?, hour: Int, minute: Int, onDone: () -> Unit, onError: (String) -> Unit) {
        validationError(hour, minute, editingId)?.let(onError) ?: run {
            viewModelScope.launch {
                val curr = container.reminderRepository.list()
                val next = if (editingId == null) {
                    curr + ReminderSchedule(
                        id = ReminderRules.nextId(curr),
                        hour = hour,
                        minute = minute,
                        enabled = true,
                    )
                } else {
                    curr.map { if (it.id == editingId) it.copy(hour = hour, minute = minute) else it }
                }
                container.reminderRepository.save(next)
                rescheduleFromRepository()
                refreshPermission()
                onDone()
            }
        }
    }

    fun delete(id: Long, onDone: () -> Unit) = viewModelScope.launch {
        container.reminderRepository.delete(id)
        rescheduleFromRepository()
        refreshPermission()
        onDone()
    }

    fun setEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        container.reminderRepository.setEnabled(id, enabled)
        rescheduleFromRepository()
        refreshPermission()
    }

    private suspend fun rescheduleFromRepository() {
        if (isReminderSupportedOnPlatform) {
            container.notificationScheduler.rescheduleAll(container.reminderRepository.list())
        }
    }
}
