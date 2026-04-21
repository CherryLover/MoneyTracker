package com.chaos.bin.mt.ui.entry

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaos.bin.mt.data.Account
import com.chaos.bin.mt.data.AppTimeZone
import com.chaos.bin.mt.data.Category
import com.chaos.bin.mt.data.RecordKind
import com.chaos.bin.mt.data.SubCategory
import com.chaos.bin.mt.data.nowInstant
import com.chaos.bin.mt.di.LocalAppContainer
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.components.AccountPickerSheet
import com.chaos.bin.mt.ui.components.DateTimePickerSheet
import com.chaos.bin.mt.ui.components.FieldLine
import com.chaos.bin.mt.ui.components.HSpace
import com.chaos.bin.mt.ui.components.Hairline
import com.chaos.bin.mt.ui.components.LineIcons
import com.chaos.bin.mt.ui.components.TypeToggle
import com.chaos.bin.mt.ui.components.VSpace
import com.chaos.bin.mt.ui.home.formatYuan
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime

@Composable
fun EntryScreen() {
    val container = LocalAppContainer.current
    val vm: EntryViewModel = viewModel { EntryViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val pendingEditId by container.pendingEditRecordId.collectAsStateWithLifecycle()
    val tabNavigator = cafe.adriel.voyager.navigator.tab.LocalTabNavigator.current

    androidx.compose.runtime.LaunchedEffect(pendingEditId) {
        pendingEditId?.let { id ->
            vm.loadForEdit(id)
            container.pendingEditRecordId.value = null
        }
    }

    EntryContent(
        state = state,
        onKindChange = vm::setKind,
        onSelectCategory = vm::selectCategory,
        onSelectSub = vm::selectSub,
        onSelectAccount = vm::selectAccount,
        onNoteChange = vm::setNote,
        onOccurredAtChange = vm::setOccurredAt,
        onTypeDigit = vm::typeDigit,
        onTypeDot = vm::typeDot,
        onBackspace = vm::backspace,
        onCancelEdit = {
            vm.cancelEditing()
            tabNavigator.current = com.chaos.bin.mt.ui.nav.HomeTab
        },
        onSave = {
            vm.save { wasEdit ->
                if (wasEdit) tabNavigator.current = com.chaos.bin.mt.ui.nav.HomeTab
            }
        },
    )
}

@Composable
private fun EntryContent(
    state: EntryUiState,
    onKindChange: (RecordKind) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectSub: (String) -> Unit,
    onSelectAccount: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onOccurredAtChange: (Instant) -> Unit,
    onTypeDigit: (Char) -> Unit,
    onTypeDot: () -> Unit,
    onBackspace: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit,
) {
    val c = LocalAppColors.current
    var showAccountPicker by remember { mutableStateOf(false) }
    var showDateTimePicker by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        // 编辑模式横幅
        if (state.isEditing) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(c.subtle)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("编辑记录", color = c.text2, fontSize = 14.sp)
                Box(Modifier.weight(1f))
                Box(
                    Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCancelEdit,
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("取消", color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // 类型切换
        Box(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            TypeToggle(
                current = if (state.kind == RecordKind.Expense) "expense" else "income",
                options = listOf("expense" to "支出", "income" to "收入"),
                onChange = { key ->
                    onKindChange(if (key == "expense") RecordKind.Expense else RecordKind.Income)
                },
            )
        }

        AmountDisplay(amount = state.amountInput)

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            CategoryGrid(
                cats = state.categories,
                activeId = state.selectedCategoryId,
                onSelect = onSelectCategory,
            )
            VSpace(4.dp)
            SubRow(
                subs = state.currentCategory?.subs.orEmpty(),
                activeId = state.selectedSubCategoryId,
                onSelect = onSelectSub,
            )
            VSpace(4.dp)
            FieldBlock(
                hasAccounts = state.hasAccounts,
                accountLabel = state.accounts.firstOrNull { it.id == state.selectedAccountId }?.name
                    ?: "选择账户",
                note = state.note,
                onNoteChange = onNoteChange,
                occurredAtLabel = formatDateTime(state.occurredAt ?: nowInstant()),
                onDateTimeClick = { showDateTimePicker = true },
                onAccountClick = { showAccountPicker = true },
            )
        }

        Keypad(
            onDigit = onTypeDigit,
            onDot = onTypeDot,
            onBackspace = onBackspace,
            onDone = onSave,
        )
    }

    if (showAccountPicker) {
        AccountPickerSheet(
            accounts = state.accounts,
            selectedId = state.selectedAccountId,
            onPick = onSelectAccount,
            onDismiss = { showAccountPicker = false },
        )
    }

    if (showDateTimePicker) {
        DateTimePickerSheet(
            initial = state.occurredAt ?: nowInstant(),
            onConfirm = {
                onOccurredAtChange(it)
                showDateTimePicker = false
            },
            onDismiss = { showDateTimePicker = false },
        )
    }
}

private fun formatDateTime(at: Instant): String {
    val dt = at.toLocalDateTime(AppTimeZone)
    val today = nowInstant().toLocalDateTime(AppTimeZone).date
    val mm = dt.monthNumber.toString().padStart(2, '0')
    val dd = dt.dayOfMonth.toString().padStart(2, '0')
    val hh = dt.hour.toString().padStart(2, '0')
    val min = dt.minute.toString().padStart(2, '0')
    return if (dt.date == today) {
        "今天 $hh:$min"
    } else {
        "$mm-$dd $hh:$min"
    }
}

@Composable
private fun AmountDisplay(amount: String) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("¥", color = c.text2, fontSize = 23.sp, fontWeight = FontWeight.Light)
            HSpace(4.dp)
            Text(
                text = amount,
                color = c.text,
                fontSize = 50.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        VSpace(10.dp)
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
    }
}

@Composable
private fun CategoryGrid(
    cats: List<Category>,
    activeId: String?,
    onSelect: (String) -> Unit,
) {
    val c = LocalAppColors.current
    val rows = cats.chunked(5)
    Column {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { cat ->
                    val active = cat.id == activeId
                    Column(
                        Modifier
                            .weight(1f)
                            .background(
                                color = if (active) c.subtle else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelect(cat.id) },
                            )
                            .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
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
                                fontSize = 21.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Text(
                            cat.name,
                            color = if (active) c.text else c.text2,
                            fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        )
                    }
                }
                repeat(5 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SubRow(
    subs: List<SubCategory>,
    activeId: String?,
    onSelect: (String) -> Unit,
) {
    val c = LocalAppColors.current
    LazyRow(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(subs) { s ->
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
                        onClick = { onSelect(s.id) },
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    s.name,
                    color = if (active) c.accentText else c.text2,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun FieldBlock(
    hasAccounts: Boolean,
    accountLabel: String,
    note: String,
    onNoteChange: (String) -> Unit,
    occurredAtLabel: String,
    onDateTimeClick: () -> Unit,
    onAccountClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp)) {
        Hairline()
        FieldLine(icon = LineIcons.Cal, label = occurredAtLabel, onClick = onDateTimeClick)
        Hairline()
        if (hasAccounts) {
            FieldLine(icon = LineIcons.Wallet, label = accountLabel, onClick = onAccountClick)
            Hairline()
        }
        NoteInlineField(value = note, onChange = onNoteChange)
    }
}

@Composable
private fun NoteInlineField(value: String, onChange: (String) -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(LineIcons.Edit, null, tint = c.text3, modifier = Modifier.size(15.dp))
        HSpace(8.dp)
        Box(Modifier.weight(1f)) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = c.text2,
                    fontSize = 14.sp,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent),
                modifier = Modifier.fillMaxWidth(),
            )
            if (value.isEmpty()) {
                Text("添加备注...", color = c.text3, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (Char) -> Unit,
    onDot: () -> Unit,
    onBackspace: () -> Unit,
    onDone: () -> Unit,
) {
    val c = LocalAppColors.current
    val rows = listOf(
        listOf("7", "8", "9", "←"),
        listOf("4", "5", "6", "+"),
        listOf("1", "2", "3", "-"),
        listOf(".", "0", "再记", "完成"),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.surface)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Hairline()
        VSpace(2.dp)
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { k ->
                    val isAction = k == "再记" || k == "完成"
                    val isPrimary = k == "完成"
                    val isDel = k == "←"
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(
                                color = when {
                                    isPrimary -> c.accent
                                    isAction || isDel || k == "+" || k == "-" -> c.subtle
                                    else -> c.surface
                                },
                                shape = RoundedCornerShape(8.dp),
                            )
                            .then(
                                if (!isPrimary) Modifier.border(
                                    1.dp,
                                    c.hairline,
                                    RoundedCornerShape(8.dp),
                                ) else Modifier,
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    when (k) {
                                        "←" -> onBackspace()
                                        "." -> onDot()
                                        "完成" -> onDone()
                                        "再记" -> onDone()
                                        "+", "-" -> {} // 暂不处理表达式
                                        else -> onDigit(k[0])
                                    }
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isDel) {
                            Icon(LineIcons.Del, null, tint = c.text, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                k,
                                color = if (isPrimary) c.accentText else c.text,
                                fontSize = if (isAction) 14.sp else 18.sp,
                                fontWeight = if (isAction) FontWeight.Medium else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}
