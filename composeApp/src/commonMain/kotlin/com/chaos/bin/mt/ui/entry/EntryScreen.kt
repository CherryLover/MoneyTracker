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
import com.chaos.bin.mt.data.Category
import com.chaos.bin.mt.data.MockData
import com.chaos.bin.mt.data.SubCategory
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.components.FieldLine
import com.chaos.bin.mt.ui.components.HSpace
import com.chaos.bin.mt.ui.components.Hairline
import com.chaos.bin.mt.ui.components.LineIcons
import com.chaos.bin.mt.ui.components.TypeToggle
import com.chaos.bin.mt.ui.components.VSpace

@Composable
fun EntryScreen(hasAccounts: Boolean = true) {
    val c = LocalAppColors.current
    var tab by remember { mutableStateOf("expense") } // expense | income
    val amount by remember { mutableStateOf("38.50") }
    var catId by remember { mutableStateOf("food") }
    var subId by remember { mutableStateOf("food-lunch") }

    val cats = if (tab == "expense") MockData.expenseCategories else MockData.incomeCategories
    val currentCat = cats.firstOrNull { it.id == catId } ?: cats.first()
    val subs = currentCat.subs
    val currentSub = subs.firstOrNull { it.id == subId } ?: subs.firstOrNull()

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        // 顶栏：类型切换
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            TypeToggle(
                current = tab,
                options = listOf("expense" to "支出", "income" to "收入"),
                onChange = { next ->
                    tab = next
                    val list = if (next == "expense") MockData.expenseCategories else MockData.incomeCategories
                    val first = list.first()
                    catId = first.id
                    subId = first.subs.first().id
                },
            )
        }

        // 金额展示
        AmountDisplay(
            amount = amount,
            categoryName = currentCat.name,
            subName = currentSub?.name.orEmpty(),
        )

        // 分类 + 小类 + 字段
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            CategoryGrid(
                cats = cats,
                activeId = catId,
                onSelect = { id ->
                    catId = id
                    val c2 = cats.firstOrNull { it.id == id } ?: cats.first()
                    subId = c2.subs.first().id
                },
            )
            VSpace(4.dp)
            SubRow(subs = subs, activeId = subId, onSelect = { subId = it })
            VSpace(4.dp)
            FieldBlock(hasAccounts = hasAccounts)
        }

        Keypad()
    }
}

@Composable
private fun AmountDisplay(amount: String, categoryName: String, subName: String) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(categoryName, color = c.text3, fontSize = 11.5.sp)
            HSpace(8.dp)
            Text("·", color = c.text3, fontSize = 11.5.sp)
            HSpace(8.dp)
            Text(subName, color = c.text3, fontSize = 11.5.sp)
        }
        VSpace(4.dp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("¥", color = c.text2, fontSize = 20.sp, fontWeight = FontWeight.Light)
            HSpace(4.dp)
            Text(
                text = amount,
                color = c.text,
                fontSize = 44.sp,
                fontWeight = FontWeight.Medium,
            )
            Box(Modifier.weight(1f))
            // 闪烁光标（静态呈现）
            Box(
                Modifier
                    .width(1.5.dp)
                    .height(30.dp)
                    .background(c.accent),
            )
        }
        VSpace(10.dp)
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
    }
}

@Composable
private fun CategoryGrid(
    cats: List<Category>,
    activeId: String,
    onSelect: (String) -> Unit,
) {
    val c = LocalAppColors.current
    // 5 列网格（手动布局，按行分组避免引入 LazyGrid 依赖噪音）
    val rows = cats.chunked(5)
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
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
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Text(
                            cat.name,
                            color = if (active) c.text else c.text2,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        )
                    }
                }
                // 填满列数
                repeat(5 - row.size) {
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SubRow(
    subs: List<SubCategory>,
    activeId: String,
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
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun FieldBlock(hasAccounts: Boolean) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp)) {
        Hairline()
        FieldLine(icon = LineIcons.Cal, label = "今天 12:24")
        Hairline()
        if (hasAccounts) {
            FieldLine(icon = LineIcons.Wallet, label = "微信")
            Hairline()
        }
        FieldLine(icon = LineIcons.Edit, label = "添加备注...", placeholder = true)
    }
}

@Composable
private fun Keypad() {
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
        // 顶部分割线
        Hairline()
        VSpace(2.dp)
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
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
                                    isAction || isDel -> c.subtle
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
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isDel) {
                            Icon(
                                LineIcons.Del,
                                null,
                                tint = c.text,
                                modifier = Modifier.size(20.dp),
                            )
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
