package com.chaos.bin.mt.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaos.bin.mt.data.MockData
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.components.HSpace
import com.chaos.bin.mt.ui.components.Hairline
import com.chaos.bin.mt.ui.components.LineIcons
import com.chaos.bin.mt.ui.components.PageHeader
import com.chaos.bin.mt.ui.components.VSpace

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val c = LocalAppColors.current
    var tab by remember { mutableStateOf("expense") }
    var selected by remember { mutableStateOf("food") }
    val cats = if (tab == "expense") MockData.expenseCategories else MockData.incomeCategories
    val current = cats.firstOrNull { it.id == selected } ?: cats.first()

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
            listOf("expense" to "支出", "income" to "收入").forEach { (k, label) ->
                val active = tab == k
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
                            onClick = { tab = k; selected = (if (k == "expense") MockData.expenseCategories else MockData.incomeCategories).first().id },
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
                    .width(110.dp)
                    .fillMaxHeight()
                    .background(c.subtle),
            ) {
                LazyColumn {
                    items(cats) { cat ->
                        val active = cat.id == selected
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(if (active) c.bg else Color.Transparent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { selected = cat.id },
                                ),
                        ) {
                            // 左边激活竖条
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
                                if (cat.privacy) {
                                    Icon(
                                        LineIcons.Lock,
                                        null,
                                        tint = c.text3,
                                        modifier = Modifier.size(10.dp),
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(LineIcons.Plus, null, tint = c.text3, modifier = Modifier.size(14.dp))
                            HSpace(8.dp)
                            Text("新建", color = c.text3, fontSize = 14.sp)
                        }
                    }
                }
            }

            // 右侧小类
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(c.bg),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${current.name} 的小类 · ${current.subs.size}",
                        color = c.text3,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text("排序", color = c.accent, fontSize = 13.sp)
                }

                LazyColumn(Modifier.weight(1f)) {
                    items(current.subs) { sub ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(LineIcons.Grip, null, tint = c.text3, modifier = Modifier.size(14.dp))
                            HSpace(10.dp)
                            Text(sub.name, color = c.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            if (sub.privacy) {
                                Row(
                                    Modifier
                                        .background(c.chip, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(LineIcons.Lock, null, tint = c.text3, modifier = Modifier.size(9.dp))
                                    Text("隐私", color = c.text3, fontSize = 12.sp)
                                }
                                HSpace(8.dp)
                            }
                            Icon(LineIcons.ChevR, null, tint = c.text3, modifier = Modifier.size(14.dp))
                        }
                        Hairline()
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
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
}
