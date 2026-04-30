package com.chaos.bin.mt.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaos.bin.mt.data.RecordKind
import com.chaos.bin.mt.di.LocalAppContainer
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.components.AccountPickerSheet
import com.chaos.bin.mt.ui.components.HSpace
import com.chaos.bin.mt.ui.components.Hairline
import com.chaos.bin.mt.ui.components.PageHeader
import com.chaos.bin.mt.ui.components.TypeToggle
import com.chaos.bin.mt.ui.components.VSpace

@Composable
fun AutomationEditScreen(
    ruleId: Long?,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: AutomationEditViewModel = viewModel(key = "autoedit:${ruleId ?: "new"}") {
        AutomationEditViewModel(container, ruleId)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = LocalAppColors.current

    var showAccountPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        PageHeader(
            title = if (state.isEditing) "编辑规则" else "新建规则",
            onBack = onBack,
        )

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            // 名称
            SectionLabel("名称")
            InputField(
                value = state.draft.name,
                placeholder = "如：月薪、房租",
                onChange = vm::setName,
            )

            // 类型
            SectionLabel("类型")
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                TypeToggle(
                    current = if (state.draft.kind == RecordKind.Expense) "expense" else "income",
                    options = listOf("expense" to "支出", "income" to "收入"),
                    onChange = { key ->
                        vm.setKind(if (key == "expense") RecordKind.Expense else RecordKind.Income)
                    },
                )
            }

            // 金额
            SectionLabel("金额（元）")
            InputField(
                value = state.draft.amountInput,
                placeholder = "0",
                keyboard = KeyboardType.Decimal,
                onChange = vm::setAmountInput,
            )

            // 分类
            SectionLabel("大类")
            CategoryRow(
                cats = state.categories,
                activeId = state.draft.categoryId,
                onPick = vm::setCategory,
            )
            val subs = state.currentCategory?.subs.orEmpty()
            if (subs.isNotEmpty()) {
                SectionLabel("小类")
                SubRowPicker(
                    subs = subs,
                    activeId = state.draft.subCategoryId,
                    onPick = vm::setSub,
                )
            }

            // 账户
            SectionLabel("账户")
            val accountLabel = state.accounts.firstOrNull { it.id == state.draft.accountId }?.name
                ?: "选择账户"
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(c.surface, RoundedCornerShape(10.dp))
                    .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showAccountPicker = true },
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(accountLabel, color = c.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("选择", color = c.accent, fontSize = 13.sp)
            }

            // 备注
            SectionLabel("备注")
            InputField(
                value = state.draft.note,
                placeholder = "可选",
                onChange = vm::setNote,
            )

            // 触发规则
            SectionLabel("触发规则")
            TriggerTabsRow(
                current = state.draft.triggerTab,
                onPick = vm::setTriggerTab,
            )
            VSpace(8.dp)
            when (state.draft.triggerTab) {
                TriggerTab.Weekly -> WeekdayChips(
                    mask = state.draft.weekdaysMask,
                    onToggle = vm::toggleWeekday,
                )
                TriggerTab.MonthlyDays -> MonthDayChips(
                    mask = state.draft.monthDaysMask,
                    onToggle = vm::toggleMonthDay,
                )
                TriggerTab.Interval -> IntervalStepper(
                    value = state.draft.intervalDays,
                    onChange = vm::setIntervalDays,
                )
            }

            // 时间
            SectionLabel("触发时间 (HH:mm)")
            TimeStepper(
                hour = state.draft.hour,
                minute = state.draft.minute,
                onHour = vm::setHour,
                onMinute = vm::setMinute,
            )

            VSpace(24.dp)
            Hairline()
            VSpace(16.dp)

            // 保存按钮
            val saveEnabled = state.isValid
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(46.dp)
                    .background(
                        if (saveEnabled) c.accent else c.subtle,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = saveEnabled,
                        onClick = { vm.save { onBack() } },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "保存",
                    color = if (saveEnabled) c.accentText else c.text3,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            // 删除按钮（编辑态）
            if (state.isEditing) {
                VSpace(10.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(46.dp)
                        .background(Color.Transparent, RoundedCornerShape(10.dp))
                        .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showDeleteConfirm = true },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("删除此规则", color = c.expense, fontSize = 15.sp)
                }
            }
            VSpace(24.dp)
        }
    }

    if (showAccountPicker) {
        AccountPickerSheet(
            accounts = state.accounts,
            selectedId = state.draft.accountId,
            onPick = vm::setAccount,
            onDismiss = { showAccountPicker = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("删除后无法恢复，已生成的记录保留。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.delete { onBack() }
                }) { Text("确认删除", color = c.expense) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    val c = LocalAppColors.current
    Text(
        text,
        color = c.text3,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun InputField(
    value: String,
    placeholder: String,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    val c = LocalAppColors.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(c.surface, RoundedCornerShape(10.dp))
            .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboard,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
            ),
            textStyle = TextStyle(color = c.text, fontSize = 15.sp),
            cursorBrush = SolidColor(c.accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = c.text3, fontSize = 15.sp)
                }
                inner()
            },
        )
    }
}

@Composable
private fun CategoryRow(
    cats: List<com.chaos.bin.mt.data.Category>,
    activeId: String?,
    onPick: (String) -> Unit,
) {
    val c = LocalAppColors.current
    LazyRow(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(cats, key = { it.id }) { cat ->
            val active = cat.id == activeId
            Column(
                Modifier
                    .background(
                        color = if (active) c.subtle else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onPick(cat.id) },
                    )
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .background(
                            color = if (active) c.accent else c.chip,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        cat.emoji,
                        color = if (active) c.accentText else c.text,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                VSpace(4.dp)
                Text(
                    cat.name,
                    color = if (active) c.text else c.text2,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun SubRowPicker(
    subs: List<com.chaos.bin.mt.data.SubCategory>,
    activeId: String?,
    onPick: (String?) -> Unit,
) {
    val c = LocalAppColors.current
    LazyRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(subs, key = { it.id }) { s ->
            val active = s.id == activeId
            Box(
                Modifier
                    .background(
                        color = if (active) c.accent else Color.Transparent,
                        shape = RoundedCornerShape(999.dp),
                    )
                    .border(
                        1.dp,
                        color = if (active) c.accent else c.line,
                        shape = RoundedCornerShape(999.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onPick(if (active) null else s.id) },
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    s.name,
                    color = if (active) c.accentText else c.text2,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun TriggerTabsRow(current: TriggerTab, onPick: (TriggerTab) -> Unit) {
    val c = LocalAppColors.current
    val items = listOf(
        TriggerTab.Weekly to "每周",
        TriggerTab.MonthlyDays to "每月",
        TriggerTab.Interval to "每隔",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(c.subtle, RoundedCornerShape(999.dp))
            .border(1.dp, c.hairline, RoundedCornerShape(999.dp))
            .padding(3.dp),
    ) {
        items.forEach { (key, label) ->
            val active = current == key
            Box(
                Modifier
                    .weight(1f)
                    .background(
                        color = if (active) c.accent else Color.Transparent,
                        shape = RoundedCornerShape(999.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onPick(key) },
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (active) c.accentText else c.text2,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private val WeekdayShort = listOf("日", "一", "二", "三", "四", "五", "六")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdayChips(mask: Int, onToggle: (Int) -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (bit in 0..6) {
            val active = (mask and (1 shl bit)) != 0
            Box(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(
                        color = if (active) c.accent else c.surface,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .border(
                        1.dp,
                        color = if (active) c.accent else c.hairline,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onToggle(bit) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    WeekdayShort[bit],
                    color = if (active) c.accentText else c.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthDayChips(mask: Int, onToggle: (Int) -> Unit) {
    val c = LocalAppColors.current
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (bit in 0..30) {
            val active = (mask and (1 shl bit)) != 0
            Box(
                Modifier
                    .size(40.dp)
                    .background(
                        color = if (active) c.accent else c.surface,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .border(
                        1.dp,
                        color = if (active) c.accent else c.hairline,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onToggle(bit) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (bit + 1).toString(),
                    color = if (active) c.accentText else c.text,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun IntervalStepper(value: Int, onChange: (Int) -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton("−") { onChange((value - 1).coerceAtLeast(1)) }
        HSpace(10.dp)
        Box(
            Modifier
                .weight(1f)
                .height(44.dp)
                .background(c.surface, RoundedCornerShape(10.dp))
                .border(1.dp, c.hairline, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("每隔 $value 天", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        HSpace(10.dp)
        StepperButton("+") { onChange((value + 1).coerceAtMost(365)) }
    }
}

@Composable
private fun TimeStepper(hour: Int, minute: Int, onHour: (Int) -> Unit, onMinute: (Int) -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeColumn(
            label = "时",
            value = hour,
            maxExclusive = 24,
            onChange = onHour,
        )
        HSpace(10.dp)
        Text(":", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        HSpace(10.dp)
        TimeColumn(
            label = "分",
            value = minute,
            maxExclusive = 60,
            onChange = onMinute,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TimeColumn(
    label: String,
    value: Int,
    maxExclusive: Int,
    onChange: (Int) -> Unit,
) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .weight(1f)
            .background(c.surface, RoundedCornerShape(10.dp))
            .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton("−") { onChange(((value - 1) + maxExclusive) % maxExclusive) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                value.toString().padStart(2, '0'),
                color = c.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        StepperButton("+") { onChange((value + 1) % maxExclusive) }
    }
}

@Composable
private fun StepperButton(sign: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        Modifier
            .size(34.dp)
            .background(c.subtle, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(sign, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}
