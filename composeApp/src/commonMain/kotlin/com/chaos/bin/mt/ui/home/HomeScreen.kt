package com.chaos.bin.mt.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaos.bin.mt.data.DayRecords
import com.chaos.bin.mt.data.MockData
import com.chaos.bin.mt.data.MoneyRecord
import com.chaos.bin.mt.data.RecordType
import com.chaos.bin.mt.theme.AppColors
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.components.EmojiChip
import com.chaos.bin.mt.ui.components.HSpace
import com.chaos.bin.mt.ui.components.Hairline
import com.chaos.bin.mt.ui.components.LineIcons
import com.chaos.bin.mt.ui.components.VSpace

@Composable
fun HomeScreen(
    showPrivacy: Boolean = false,
    hasAccounts: Boolean = true,
) {
    val c = LocalAppColors.current
    val (exp, inc, bal) = MockData.sumMonth()
    val mask = showPrivacy

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        item { MonthHeader() }
        item { BoardWarm(exp = exp, inc = inc, bal = bal, mask = mask) }
        // 列表
        MockData.records.forEach { day ->
            item(key = "header-${day.day}") {
                DayHeader(day = day, mask = mask)
            }
            items(day.items, key = { it.id }) { rec ->
                RecordRow(rec = rec, mask = mask, hasAccounts = hasAccounts)
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Text("—— 到底了 ——", fontSize = 12.sp, color = c.text3)
            }
        }
    }
}

@Composable
private fun MonthHeader() {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "2026 年 4 月",
                color = c.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            HSpace(8.dp)
            Icon(LineIcons.ChevD, null, tint = c.text3, modifier = Modifier.size(14.dp))
        }
        Box(Modifier.weight(1f))
        Icon(LineIcons.ChevL, null, tint = c.text2, modifier = Modifier.size(18.dp))
        HSpace(14.dp)
        Icon(
            LineIcons.ChevR,
            null,
            tint = c.text2.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun BoardWarm(exp: Int, inc: Int, bal: Int, mask: Boolean) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.surface, RoundedCornerShape(16.dp))
                .border(1.dp, c.hairline, RoundedCornerShape(16.dp))
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("本月支出", color = c.text2, fontSize = 12.sp)
                HSpace(8.dp)
                Box(
                    Modifier
                        .size(3.dp)
                        .background(c.accent, CircleShape),
                )
            }
            VSpace(2.dp)
            Text(
                text = if (mask) "¥•••••" else "¥" + formatThousands(exp),
                color = c.text,
                fontSize = 40.sp,
                fontWeight = FontWeight.Medium,
            )
            VSpace(14.dp)
            MiniBarChart()
            VSpace(14.dp)
            Hairline()
            VSpace(14.dp)
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("收入", color = c.text2, fontSize = 11.sp)
                    VSpace(3.dp)
                    Text(
                        text = if (mask) "¥•••" else "¥" + formatThousands(inc),
                        color = c.income,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("结余", color = c.text2, fontSize = 11.sp)
                    VSpace(3.dp)
                    Text(
                        text = if (mask) "¥•••" else "¥" + formatThousands(bal),
                        color = c.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniBarChart() {
    val c = LocalAppColors.current
    val heights = listOf(0.3f, 0.5f, 0.4f, 0.7f, 0.45f, 0.6f, 0.9f, 0.55f, 0.7f, 0.5f, 0.8f, 0.65f, 0.4f, 0.75f)
    Row(
        Modifier.fillMaxWidth().height(26.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEachIndexed { i, h ->
            Box(
                Modifier
                    .weight(1f)
                    .height((26f * h).dp)
                    .background(
                        color = if (i == 6) c.accent else c.subtle,
                        shape = RoundedCornerShape(1.dp),
                    ),
            )
        }
    }
}

@Composable
private fun DayHeader(day: DayRecords, mask: Boolean) {
    val c = LocalAppColors.current
    val dayExp = day.items.filter { it.type == RecordType.Expense }.sumOf { it.amount }
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.bg),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "4 月 ${day.day} 日",
                color = c.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            HSpace(10.dp)
            Text(
                text = day.weekday,
                color = c.text3,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 2.dp),
            )
            Box(Modifier.weight(1f))
            Text(
                text = "支出 " + if (mask) "¥•••" else "¥$dayExp",
                color = c.text2,
                fontSize = 11.5.sp,
            )
        }
        Hairline()
    }
}

@Composable
private fun RecordRow(rec: MoneyRecord, mask: Boolean, hasAccounts: Boolean) {
    val c = LocalAppColors.current
    val isIncome = rec.type == RecordType.Income
    val isPrivate = rec.privacy
    val amountStr = when {
        mask || isPrivate -> "••••"
        else -> (if (isIncome) "+" else "−") + formatThousands(rec.amount)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiChip(emoji = rec.emoji, colors = c)
        HSpace(12.dp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rec.cat, color = c.text, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                Text(" · ${rec.sub}", color = c.text2, fontSize = 13.sp)
                if (isPrivate) {
                    HSpace(5.dp)
                    Icon(
                        LineIcons.Lock,
                        null,
                        tint = c.text3,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
            VSpace(2.dp)
            Text(
                text = buildString {
                    append(rec.time)
                    if (rec.note.isNotBlank()) append(" · ").append(rec.note)
                    if (hasAccounts) append(" · ").append(rec.account)
                },
                color = c.text2,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HSpace(8.dp)
        Text(
            text = amountStr,
            color = if (isIncome) c.income else c.expense,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

internal fun formatThousands(n: Int): String {
    val s = kotlin.math.abs(n).toString()
    val sb = StringBuilder()
    if (n < 0) sb.append('-')
    val rem = s.length % 3
    s.forEachIndexed { i, ch ->
        if (i != 0 && (i - rem) % 3 == 0) sb.append(',')
        sb.append(ch)
    }
    return sb.toString()
}
