package com.chaos.bin.mt.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaos.bin.mt.data.Account
import com.chaos.bin.mt.di.LocalAppContainer
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.components.EmojiChip
import com.chaos.bin.mt.ui.components.EmptyState
import com.chaos.bin.mt.ui.components.HSpace
import com.chaos.bin.mt.ui.components.Hairline
import com.chaos.bin.mt.ui.components.LineIcons
import com.chaos.bin.mt.ui.components.PageHeader
import com.chaos.bin.mt.ui.components.VSpace

private val EmojiList = listOf("💳", "🏦", "💰", "💵", "💸", "🐷", "🏧", "🪙", "🔵", "🟡", "🟢", "⭐")

@Composable
fun AccountsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: AccountsViewModel = viewModel { AccountsViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Account?>(null) }

    val c = LocalAppColors.current

    Column(Modifier.fillMaxSize().background(c.bg)) {
        PageHeader(
            title = "账户管理",
            onBack = onBack,
            right = {
                Icon(
                    imageVector = LineIcons.Plus,
                    contentDescription = "添加账户",
                    tint = c.accent,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showAdd = true },
                        ),
                )
            },
        )

        if (state.accounts.isEmpty()) {
            EmptyState(
                icon = LineIcons.Wallet,
                title = "还没有账户",
                description = "添加账户后可以在记一笔时选择",
                actionLabel = "添加账户",
                onAction = { showAdd = true },
            )
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface),
            ) {
                Hairline()
                LazyColumn {
                    items(state.accounts, key = { it.id }) { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { editTarget = account },
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            EmojiChip(account.emoji, 36.dp)
                            HSpace(12.dp)
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = account.name,
                                    color = c.text,
                                    fontSize = 16.sp,
                                )
                                if (account.id == state.defaultId) {
                                    HSpace(6.dp)
                                    Text(
                                        text = "默认",
                                        color = c.accent,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .background(c.subtle, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Icon(
                                imageVector = LineIcons.ChevR,
                                contentDescription = null,
                                tint = c.text3,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Hairline()
                    }
                }
            }
        }
    }

    if (showAdd) {
        AccountEditSheet(
            account = null,
            isDefault = false,
            isFirst = state.accounts.isEmpty(),
            canDelete = true,
            onDismiss = { showAdd = false },
            onSave = { name, emoji -> vm.add(name, emoji) },
            onSetDefault = {},
            onDelete = {},
        )
    }

    editTarget?.let { account ->
        var canDelete by remember { mutableStateOf<Boolean?>(null) }
        LaunchedEffect(account.id) {
            canDelete = !vm.hasRecords(account.id)
        }
        AccountEditSheet(
            account = account,
            isDefault = account.id == state.defaultId,
            isFirst = false,
            canDelete = canDelete,
            onDismiss = { editTarget = null },
            onSave = { name, emoji -> vm.update(account.id, name, emoji) },
            onSetDefault = { vm.setDefault(account.id) },
            onDelete = { vm.delete(account.id) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountEditSheet(
    account: Account?,
    isDefault: Boolean,
    isFirst: Boolean,
    canDelete: Boolean?,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String) -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialName = when {
        account != null -> account.name
        isFirst -> "默认账户"
        else -> ""
    }
    val initialEmoji = account?.emoji ?: "💳"

    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isAddMode = account == null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        contentColor = c.text,
    ) {
        Column(Modifier.fillMaxWidth()) {
            // 标题
            Text(
                text = if (isAddMode) "添加账户" else "编辑账户",
                color = c.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            Hairline()

            // Emoji + 名称输入区
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Emoji 选择方块
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(c.chip, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emoji, fontSize = 22.sp)
                }
                HSpace(12.dp)
                // 名称输入
                Column(Modifier.weight(1f)) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = TextStyle(
                            color = c.text,
                            fontSize = 16.sp,
                        ),
                        cursorBrush = SolidColor(c.accent),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box {
                                if (name.isEmpty()) {
                                    Text("账户名称", color = c.text3, fontSize = 16.sp)
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    VSpace(4.dp)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(c.line),
                    )
                }
            }

            // Emoji 选择器
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(EmojiList) { emojiItem ->
                    val selected = emoji == emojiItem
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (selected) c.accent else c.chip,
                                shape = RoundedCornerShape(18.dp),
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { emoji = emojiItem },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emojiItem, fontSize = 18.sp)
                    }
                }
            }

            Hairline()

            // 设为默认行（edit 模式且不是默认账户时显示）
            if (!isAddMode && !isDefault) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                onSetDefault()
                                onDismiss()
                            },
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = LineIcons.Check,
                        contentDescription = null,
                        tint = c.accent,
                        modifier = Modifier.size(16.dp),
                    )
                    HSpace(10.dp)
                    Text("设为默认账户", color = c.accent, fontSize = 15.sp)
                }
                Hairline()
            }

            // 底部操作区
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 保存按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(c.accent, RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                onSave(name, emoji)
                                onDismiss()
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "保存",
                        color = c.accentText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // 删除按钮（仅 edit 模式）
                if (!isAddMode) {
                    when (canDelete) {
                        null -> {
                            // 检查中
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .background(Color.Transparent, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "检查中...",
                                    color = c.text3,
                                    fontSize = 15.sp,
                                )
                            }
                        }
                        true -> {
                            // 可删除
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .background(Color.Transparent, RoundedCornerShape(10.dp))
                                    .then(
                                        Modifier.padding(1.dp).background(
                                            Color.Transparent,
                                            RoundedCornerShape(10.dp),
                                        )
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { showDeleteConfirm = true },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(
                                            Modifier.background(
                                                Color.Transparent,
                                                RoundedCornerShape(10.dp),
                                            )
                                        ),
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .background(Color.Transparent, RoundedCornerShape(10.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { showDeleteConfirm = true },
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        "删除此账户",
                                        color = c.expense,
                                        fontSize = 15.sp,
                                    )
                                }
                            }
                        }
                        false -> {
                            // 不可删除（有关联记录）
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .background(Color.Transparent, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "有关联记录，无法删除",
                                    color = c.text3,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("删除后无法恢复，请确认。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                        onDismiss()
                    },
                ) {
                    Text("确认删除", color = c.expense)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            },
        )
    }
}
