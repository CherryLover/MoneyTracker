package com.chaos.bin.mt.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaos.bin.mt.data.Category
import com.chaos.bin.mt.data.RecordKind
import com.chaos.bin.mt.data.SubCategory
import com.chaos.bin.mt.di.LocalAppContainer
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.components.DragReorderColumn
import com.chaos.bin.mt.ui.components.HSpace
import com.chaos.bin.mt.ui.components.Hairline
import com.chaos.bin.mt.ui.components.LineIcons
import com.chaos.bin.mt.ui.components.PageHeader
import com.chaos.bin.mt.ui.components.VSpace

private val CategoryEmojiList = listOf(
    "🍚", "🚗", "🛍️", "💊", "🏠", "🎁", "📚", "🎬",
    "💼", "💡", "📈", "✨", "🍰", "🎨", "✈️", "📱",
    "⚽", "🐾", "💰", "🌿", "🎵", "🏷️",
)

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: CategoriesViewModel = viewModel { CategoriesViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = LocalAppColors.current

    var showAddCategory by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf<Category?>(null) }
    var showAddSub by remember { mutableStateOf(false) }
    var editSub by remember { mutableStateOf<SubCategory?>(null) }
    var categoryPanelWidth by remember { mutableStateOf(110.dp) }
    val density = LocalDensity.current

    val current = state.currentCategory

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        PageHeader(title = "分类管理", onBack = onBack)

        // 类型 pill
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf(RecordKind.Expense to "支出", RecordKind.Income to "收入").forEach { (k, label) ->
                val active = state.kind == k
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
                            onClick = { vm.switchKind(k) },
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        label,
                        color = if (active) c.bg else c.text2,
                        fontSize = 15.sp,
                    )
                }
            }
        }

        Row(Modifier.fillMaxSize()) {
            // 左侧大类列
            Column(
                Modifier
                    .width(categoryPanelWidth)
                    .fillMaxHeight()
                    .background(c.subtle)
                    .verticalScroll(rememberScrollState()),
            ) {
                DragReorderColumn(
                    items = state.currentList,
                    key = { it.id },
                    onReorder = { newList -> vm.reorderCategories(newList.map { it.id }) },
                ) { cat, isDragging, handle ->
                    val active = cat.id == current?.id
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                when {
                                    isDragging -> c.surface
                                    active -> c.bg
                                    else -> Color.Transparent
                                },
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { vm.select(cat.id) },
                            ),
                    ) {
                        if (active) {
                            Box(
                                Modifier
                                    .width(2.dp)
                                    .height(46.dp)
                                    .background(c.accent),
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(cat.emoji, fontSize = 18.sp)
                            HSpace(8.dp)
                            Text(
                                cat.name,
                                color = if (active) c.text else c.text2,
                                fontSize = 15.sp,
                                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                LineIcons.Grip,
                                contentDescription = "拖动排序",
                                tint = c.text3,
                                modifier = handle.size(14.dp),
                            )
                        }
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showAddCategory = true },
                        )
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(LineIcons.Plus, null, tint = c.accent, modifier = Modifier.size(14.dp))
                    HSpace(8.dp)
                    Text("新建", color = c.accent, fontSize = 14.sp)
                }
            }

            // 左右分割线（可水平拖拽调整左栏宽度）
            Box(
                Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dx ->
                            change.consume()
                            val delta = with(density) { dx.toDp() }
                            categoryPanelWidth = (categoryPanelWidth + delta)
                                .coerceIn(90.dp, 220.dp)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(c.hairline),
                )
                Box(
                    Modifier
                        .size(width = 4.dp, height = 28.dp)
                        .background(c.text3, RoundedCornerShape(2.dp)),
                )
            }

            // 右侧小类
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(c.bg),
            ) {
                if (current == null) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "点击左侧「新建」添加第一个大类",
                            color = c.text3,
                            fontSize = 14.sp,
                        )
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(
                            start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${current.name} 的小类 · ${current.subs.size}",
                            color = c.text3,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            LineIcons.Edit,
                            contentDescription = "编辑当前大类",
                            tint = c.accent,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { editCategory = current },
                                ),
                        )
                    }

                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        DragReorderColumn(
                            items = current.subs,
                            key = { it.id },
                            onReorder = { newList -> vm.reorderSubs(newList.map { it.id }) },
                        ) { sub, isDragging, handle ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(if (isDragging) c.subtle else Color.Transparent)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { editSub = sub },
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    LineIcons.Grip,
                                    contentDescription = "拖动排序",
                                    tint = c.text3,
                                    modifier = handle.size(14.dp),
                                )
                                HSpace(10.dp)
                                Text(sub.name, color = c.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                Icon(LineIcons.ChevR, null, tint = c.text3, modifier = Modifier.size(14.dp))
                            }
                            Hairline()
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { showAddSub = true },
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(LineIcons.Plus, null, tint = c.accent, modifier = Modifier.size(14.dp))
                            HSpace(8.dp)
                            Text("添加小类", color = c.accent, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddCategory) {
        CategoryEditSheet(
            category = null,
            canDelete = null,
            onDismiss = { showAddCategory = false },
            onSave = { name, emoji -> vm.addCategory(name, emoji) },
            onDelete = {},
        )
    }

    editCategory?.let { cat ->
        var canDelete by remember(cat.id) { mutableStateOf<Boolean?>(null) }
        LaunchedEffect(cat.id) { canDelete = !vm.hasRecordsForCategory(cat.id) }
        CategoryEditSheet(
            category = cat,
            canDelete = canDelete,
            onDismiss = { editCategory = null },
            onSave = { name, emoji -> vm.updateCategory(cat.id, name, emoji) },
            onDelete = { vm.deleteCategory(cat.id) },
        )
    }

    if (showAddSub && current != null) {
        SubEditSheet(
            sub = null,
            canDelete = null,
            onDismiss = { showAddSub = false },
            onSave = { name -> vm.addSub(current.id, name) },
            onDelete = {},
        )
    }

    editSub?.let { sub ->
        var canDelete by remember(sub.id) { mutableStateOf<Boolean?>(null) }
        LaunchedEffect(sub.id) { canDelete = !vm.hasRecordsForSub(sub.id) }
        SubEditSheet(
            sub = sub,
            canDelete = canDelete,
            onDismiss = { editSub = null },
            onSave = { name -> vm.updateSub(sub.id, name) },
            onDelete = { vm.deleteSub(sub.id) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditSheet(
    category: Category?,
    canDelete: Boolean?,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String) -> Unit,
    onDelete: () -> Unit,
) {
    val c = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isAddMode = category == null
    var name by remember { mutableStateOf(category?.name ?: "") }
    var emoji by remember { mutableStateOf(category?.emoji ?: "🏷️") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val submit: () -> Unit = {
        if (name.isNotBlank()) {
            onSave(name, emoji)
            onDismiss()
        }
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        contentColor = c.text,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = if (isAddMode) "添加大类" else "编辑大类",
                color = c.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            Hairline()

            // emoji + 名称
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(44.dp).background(c.chip, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text(emoji, fontSize = 22.sp) }
                HSpace(12.dp)
                Column(Modifier.weight(1f)) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = TextStyle(color = c.text, fontSize = 16.sp),
                        cursorBrush = SolidColor(c.accent),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        decorationBox = { inner ->
                            Box {
                                if (name.isEmpty()) Text("大类名称", color = c.text3, fontSize = 16.sp)
                                inner()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    VSpace(4.dp)
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                }
            }

            // emoji 选择器
            LazyRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(CategoryEmojiList) { item ->
                    val selected = emoji == item
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(
                                color = if (selected) c.accent else c.chip,
                                shape = RoundedCornerShape(18.dp),
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { emoji = item },
                            ),
                        contentAlignment = Alignment.Center,
                    ) { Text(item, fontSize = 18.sp) }
                }
            }

            Hairline()

            // 底部操作
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(c.accent, RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { submit() },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("保存", color = c.accentText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                if (!isAddMode) {
                    DeleteRow(
                        label = "删除此大类",
                        lockedMessage = "下面有记录，无法删除",
                        canDelete = canDelete,
                        onClick = { showDeleteConfirm = true },
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("大类及其下所有小类都会被删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                    onDismiss()
                }) { Text("确认删除", color = c.expense) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubEditSheet(
    sub: SubCategory?,
    canDelete: Boolean?,
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit,
    onDelete: () -> Unit,
) {
    val c = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isAddMode = sub == null
    var name by remember { mutableStateOf(sub?.name ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val submit: () -> Unit = {
        if (name.isNotBlank()) {
            onSave(name)
            onDismiss()
        }
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        contentColor = c.text,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = if (isAddMode) "添加小类" else "编辑小类",
                color = c.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            Hairline()

            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(color = c.text, fontSize = 16.sp),
                    cursorBrush = SolidColor(c.accent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    decorationBox = { inner ->
                        Box {
                            if (name.isEmpty()) Text("小类名称", color = c.text3, fontSize = 16.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                VSpace(4.dp)
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
            }

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(c.accent, RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { submit() },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("保存", color = c.accentText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                if (!isAddMode) {
                    DeleteRow(
                        label = "删除此小类",
                        lockedMessage = "有关联记录，无法删除",
                        canDelete = canDelete,
                        onClick = { showDeleteConfirm = true },
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("删除后无法恢复，请确认。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                    onDismiss()
                }) { Text("确认删除", color = c.expense) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun DeleteRow(
    label: String,
    lockedMessage: String,
    canDelete: Boolean?,
    onClick: () -> Unit,
) {
    val c = LocalAppColors.current
    Box(
        Modifier.fillMaxWidth().height(46.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (canDelete) {
            null -> Text("检查中...", color = c.text3, fontSize = 15.sp)
            true -> Box(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) { Text(label, color = c.expense, fontSize = 15.sp) }
            false -> Text(lockedMessage, color = c.text3, fontSize = 14.sp)
        }
    }
}
