package com.chaos.bin.mt.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaos.bin.mt.data.ReminderRules
import com.chaos.bin.mt.data.ReminderSchedule
import com.chaos.bin.mt.data.formatReminderTime
import com.chaos.bin.mt.di.LocalAppContainer
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.components.EmptyState
import com.chaos.bin.mt.ui.components.HSpace
import com.chaos.bin.mt.ui.components.LineIcons
import com.chaos.bin.mt.ui.components.PageHeader
import com.chaos.bin.mt.ui.components.ThemedSwitch
import com.chaos.bin.mt.ui.components.TimeStepper
import com.chaos.bin.mt.ui.components.VSpace

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: NotificationsViewModel = viewModel { NotificationsViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = LocalAppColors.current

    var editing by remember { mutableStateOf<ReminderSchedule?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!vm.refreshPermission()) showPermissionDialog = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState()),
    ) {
        val canAdd = state.schedules.size < ReminderRules.MAX_COUNT
        PageHeader(
            title = "通知提醒",
            onBack = onBack,
            right = {
                Box(
                    Modifier
                        .clickable(
                            enabled = canAdd,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showNewDialog = true },
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text("新增", color = if (canAdd) c.accent else c.text3, fontSize = 15.sp)
                }
            },
        )

        Text(
            "到点提醒你记账，最多 3 条，相邻不少于 1 小时。",
            color = c.text2,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
        )

        if (!state.permissionGranted) {
            PermissionWarning(onOpenSettings = vm::openAppSettings)
            VSpace(10.dp)
        }

        if (state.schedules.isEmpty()) {
            EmptyState(
                icon = LineIcons.Bell,
                title = "还没有提醒",
                description = "设置每日定时提醒，到点轻轻提醒你记账",
                actionLabel = "新增提醒",
                onAction = { showNewDialog = true },
            )
        } else {
            Column(Modifier.padding(horizontal = 16.dp)) {
                state.schedules.forEach { schedule ->
                    ReminderCard(
                        schedule = schedule,
                        onClick = { editing = schedule },
                        onToggle = { vm.setEnabled(schedule.id, it) },
                    )
                    VSpace(8.dp)
                }
            }
        }
        VSpace(24.dp)
    }

    if (showNewDialog || editing != null) {
        NotificationEditDialog(
            schedule = editing,
            validate = { hour, minute, editingId -> vm.validationError(hour, minute, editingId) },
            onDismiss = {
                showNewDialog = false
                editing = null
            },
            onSave = { editingId, hour, minute ->
                vm.save(
                    editingId = editingId,
                    hour = hour,
                    minute = minute,
                    onDone = {
                        showNewDialog = false
                        editing = null
                    },
                    onError = { errorMessage = it },
                )
            },
            onDelete = { id ->
                vm.delete(id) {
                    showNewDialog = false
                    editing = null
                }
            },
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("开启记账提醒？") },
            text = { Text("允许通知后，系统会在你设置的时间提醒你记一笔。") },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text("暂不开启") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        vm.requestPermission()
                    },
                ) { Text("去开启") }
            },
        )
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("提示") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("好") }
            },
        )
    }
}

@Composable
private fun PermissionWarning(onOpenSettings: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(c.accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .border(1.dp, c.accent.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenSettings,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⚠️", fontSize = 15.sp)
        HSpace(8.dp)
        Text(
            "通知未开启，无法收到提醒。前往系统设置 →",
            color = c.text2,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ReminderCard(
    schedule: ReminderSchedule,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(12.dp))
            .border(1.dp, c.hairline, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .alpha(if (schedule.enabled) 1f else 0.55f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(LineIcons.Bell, null, tint = c.text2, modifier = Modifier.size(18.dp))
            HSpace(8.dp)
            Text(
                formatReminderTime(schedule),
                color = c.text,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            ThemedSwitch(on = schedule.enabled, onChange = onToggle)
        }
        VSpace(10.dp)
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
        VSpace(10.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                reminderLabel(schedule),
                color = c.text2,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (schedule.enabled) "已开启" else "已暂停",
                color = if (schedule.enabled) c.accent else c.text3,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun NotificationEditDialog(
    schedule: ReminderSchedule?,
    validate: (Int, Int, Long?) -> String?,
    onDismiss: () -> Unit,
    onSave: (Long?, Int, Int) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val c = LocalAppColors.current
    var hour by remember(schedule?.id) { mutableStateOf(schedule?.hour ?: 9) }
    var minute by remember(schedule?.id) { mutableStateOf(schedule?.minute ?: 0) }
    val editingId = schedule?.id
    val error = validate(hour, minute, editingId)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (schedule == null) "新增提醒" else "编辑提醒") },
        text = {
            Column {
                Text(
                    "选择每天提醒你的时间",
                    color = c.text2,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                VSpace(14.dp)
                TimeStepper(
                    hour = hour,
                    minute = minute,
                    onHour = { hour = it },
                    onMinute = { minute = it },
                )
                VSpace(10.dp)
                Text(
                    text = error ?: " ",
                    color = c.expense,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (editingId != null) {
                    TextButton(onClick = { onDelete(editingId) }) {
                        Text("删除", color = c.expense)
                    }
                }
                TextButton(
                    enabled = error == null,
                    onClick = { onSave(editingId, hour, minute) },
                ) { Text("保存") }
            }
        },
    )
}

private fun reminderLabel(schedule: ReminderSchedule): String = when (schedule.hour) {
    in 0..11 -> "早间提醒"
    in 12..17 -> "午间提醒"
    else -> "晚间提醒"
}
