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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaos.bin.mt.data.Category
import com.chaos.bin.mt.data.RecordKind
import com.chaos.bin.mt.di.LocalAppContainer
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.components.HSpace
import com.chaos.bin.mt.ui.components.Hairline
import com.chaos.bin.mt.ui.components.LineIcons
import com.chaos.bin.mt.ui.components.PageHeader
import com.chaos.bin.mt.ui.components.ThemedSwitch
import com.chaos.bin.mt.ui.components.VSpace

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: PrivacyViewModel = viewModel { PrivacyViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = LocalAppColors.current
    var showAdd by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf<PrivacyEntry?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState()),
    ) {
        PageHeader(title = "隐私保护", onBack = onBack)

        Text(
            "关闭对应开关，金额会显示为 ••••。手动添加的分类，其记录金额也会被遮蔽。",
            color = c.text2,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        )

        Group(title = "首页金额保护") {
            SwitchRow("支出", state.maskExpense) { vm.setMaskExpense(it) }
            SwitchRow("收入", state.maskIncome) { vm.setMaskIncome(it) }
            SwitchRow("结余", state.maskBalance) { vm.setMaskBalance(it) }
        }

        Group(
            title = "隐私分类",
            desc = "这些分类下的记录金额，会在列表里被遮蔽。",
        ) {
            if (state.entries.isEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text("还没有添加", color = c.text3, fontSize = 14.sp)
                }
                Hairline()
            } else {
                state.entries.forEach { entry ->
                    EntryRow(
                        emoji = entry.categoryEmoji,
                        title = if (entry.subCategoryId == null) entry.categoryName
                                else "${entry.categoryName} · ${entry.subCategoryName}",
                        badge = if (entry.subCategoryId == null) "整个大类" else null,
                        onRemove = { confirmRemove = entry },
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showAdd = true },
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(LineIcons.Plus, null, tint = c.accent, modifier = Modifier.size(14.dp))
                HSpace(8.dp)
                Text("添加隐私分类", color = c.accent, fontSize = 15.sp)
            }
        }

        VSpace(20.dp)
    }

    if (showAdd) {
        AddPrivacySheet(
            expense = state.expense,
            income = state.income,
            onPickCategory = { vm.addCategory(it) },
            onPickSub = { vm.addSub(it) },
            onDismiss = { showAdd = false },
        )
    }

    confirmRemove?.let { entry ->
        val title = if (entry.subCategoryId == null) entry.categoryName
                    else "${entry.categoryName} · ${entry.subCategoryName}"
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("移除隐私标记") },
            text = { Text("移除后「$title」下的记录金额会正常显示。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.remove(entry)
                    confirmRemove = null
                }) { Text("移除", color = c.expense) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SwitchRow(title: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalAppColors.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onChange(!on) },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = c.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
            ThemedSwitch(on = on, onChange = onChange)
        }
        Hairline()
    }
}

@Composable
private fun EntryRow(
    emoji: String,
    title: String,
    badge: String?,
    onRemove: () -> Unit,
) {
    val c = LocalAppColors.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(emoji, fontSize = 18.sp)
            HSpace(10.dp)
            Text(title, color = c.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
            if (badge != null) {
                Text(
                    badge,
                    color = c.text3,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(c.chip, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                HSpace(10.dp)
            }
            Icon(
                LineIcons.Del,
                contentDescription = "移除",
                tint = c.expense,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRemove,
                    ),
            )
        }
        Hairline()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPrivacySheet(
    expense: List<Category>,
    income: List<Category>,
    onPickCategory: (id: String) -> Unit,
    onPickSub: (id: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var kind by remember { mutableStateOf(RecordKind.Expense) }
    var picked by remember { mutableStateOf<Category?>(null) }

    val source = if (kind == RecordKind.Expense) expense else income

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        contentColor = c.text,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                "添加隐私分类",
                color = c.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            Hairline()

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(RecordKind.Expense to "支出", RecordKind.Income to "收入").forEach { (k, label) ->
                    val active = kind == k
                    Box(
                        Modifier
                            .background(
                                color = if (active) c.text else Color.Transparent,
                                shape = RoundedCornerShape(999.dp),
                            )
                            .border(
                                1.dp,
                                color = if (active) c.text else c.hairline,
                                shape = RoundedCornerShape(999.dp),
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { kind = k; picked = null },
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            label,
                            color = if (active) c.bg else c.text2,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            if (picked == null) {
                // 第 1 步：选大类
                Text(
                    "选择大类",
                    color = c.text3,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 6.dp),
                )
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    items(source, key = { it.id }) { cat ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { picked = cat },
                                )
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(cat.emoji, fontSize = 18.sp)
                            HSpace(10.dp)
                            Text(cat.name, color = c.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            if (cat.privacy) {
                                Text(
                                    "已标记",
                                    color = c.text3,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .background(c.chip, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                                HSpace(8.dp)
                            }
                            Icon(LineIcons.ChevR, null, tint = c.text3, modifier = Modifier.size(14.dp))
                        }
                        Hairline()
                    }
                }
            } else {
                val cat = picked!!
                // 第 2 步：选择整个大类 or 某个小类
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        LineIcons.ChevL,
                        contentDescription = "返回",
                        tint = c.text2,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { picked = null },
                            ),
                    )
                    HSpace(8.dp)
                    Text(cat.emoji, fontSize = 18.sp)
                    HSpace(6.dp)
                    Text(cat.name, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Hairline()

                OptionRow(
                    title = "整个大类",
                    desc = if (cat.privacy) "已经标记" else "该大类下所有记录金额都会被遮蔽",
                    enabled = !cat.privacy,
                    onClick = {
                        onPickCategory(cat.id)
                        onDismiss()
                    },
                )
                if (cat.subs.isNotEmpty()) {
                    Text(
                        "或选择小类",
                        color = c.text3,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp),
                    )
                    cat.subs.forEach { sub ->
                        OptionRow(
                            title = sub.name,
                            desc = if (sub.privacy) "已经标记" else null,
                            enabled = !sub.privacy && !cat.privacy,
                            onClick = {
                                onPickSub(sub.id)
                                onDismiss()
                            },
                        )
                    }
                }
                VSpace(12.dp)
            }
        }
    }
}

@Composable
private fun OptionRow(
    title: String,
    desc: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalAppColors.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (enabled) Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    ) else Modifier,
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (enabled) c.text else c.text3,
                    fontSize = 15.sp,
                )
                if (desc != null) {
                    Text(desc, color = c.text3, fontSize = 12.sp)
                }
            }
            if (enabled) {
                Icon(LineIcons.ChevR, null, tint = c.text3, modifier = Modifier.size(14.dp))
            }
        }
        Hairline()
    }
}

@Composable
private fun Group(
    title: String,
    desc: String? = null,
    content: @Composable () -> Unit,
) {
    val c = LocalAppColors.current
    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Text(
            title,
            color = c.text3,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.surface),
        ) {
            Hairline()
            content()
        }
        if (desc != null) {
            Text(
                desc,
                color = c.text3,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
            )
        }
    }
}
