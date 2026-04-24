package com.chaos.bin.mt.ui.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import com.chaos.bin.mt.data.DayGroup
import com.chaos.bin.mt.data.RecordDetail
import com.chaos.bin.mt.data.RecordKind
import com.chaos.bin.mt.di.LocalAppContainer
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.nav.EntryTab
import com.chaos.bin.mt.ui.components.EmojiChip
import com.chaos.bin.mt.ui.components.EmptyState
import com.chaos.bin.mt.ui.components.HSpace
import com.chaos.bin.mt.ui.components.Hairline
import com.chaos.bin.mt.ui.components.LineIcons
import com.chaos.bin.mt.ui.components.VSpace
import kotlinx.datetime.toLocalDateTime

@Composable
fun HomeScreen() {
    val container = LocalAppContainer.current
    val vm: HomeViewModel = viewModel { HomeViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    var actionTarget by remember { mutableStateOf<RecordDetail?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val tabNavigator = LocalTabNavigator.current

    HomeContent(
        state = state,
        onPrevMonth = { vm.shiftMonth(-1) },
        onNextMonth = { vm.shiftMonth(1) },
        onRecordClick = { actionTarget = it },
        onMonthHeaderClick = { showMonthPicker = true },
        onBackToCurrent = { vm.selectMonth(state.todayYear, state.todayMonth) },
    )

    actionTarget?.let { target ->
        com.chaos.bin.mt.ui.components.RecordActionsDialog(
            record = target,
            onEdit = {
                container.pendingEditRecordId.value = target.id
                tabNavigator.current = EntryTab
                actionTarget = null
            },
            onDelete = {
                vm.deleteRecord(target.id)
                actionTarget = null
            },
            onDismiss = { actionTarget = null },
        )
    }

    if (showMonthPicker) {
        com.chaos.bin.mt.ui.components.MonthPickerSheet(
            summaries = state.monthlySummaries,
            selectedYear = state.year,
            selectedMonth = state.month,
            currentYear = state.todayYear,
            currentMonth = state.todayMonth,
            onPick = { y, m ->
                vm.selectMonth(y, m)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false },
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRecordClick: (RecordDetail) -> Unit,
    onMonthHeaderClick: () -> Unit,
    onBackToCurrent: () -> Unit,
) {
    val c = LocalAppColors.current

    LazyColumn(Modifier.fillMaxSize().background(c.bg)) {
        item {
            MonthHeader(
                year = state.year,
                month = state.month,
                isOnCurrentMonth = state.isOnCurrentMonth,
                onMonthClick = onMonthHeaderClick,
                onPrev = onPrevMonth,
                onNext = onNextMonth,
                onBackToCurrent = onBackToCurrent,
            )
        }
        item {
            BoardWarm(
                expenseCents = state.summary.expenseCents,
                incomeCents = state.summary.incomeCents,
                balanceCents = state.summary.balanceCents,
                maskExpense = state.maskHomeExpense,
                maskIncome = state.maskHomeIncome,
                maskBalance = state.maskHomeBalance,
                dailyExpense = state.dailyExpenseCents,
                todayIndex = state.todayIndex,
            )
        }

        if (state.dayGroups.isEmpty()) {
            item {
                EmptyState(
                    icon = LineIcons.Plus,
                    title = "本月还没有记录",
                    description = "点下方 + 开始记账",
                )
            }
        } else {
            state.dayGroups.forEach { day ->
                item(key = "header-${day.date}") { DayHeader(day = day) }
                items(day.items, key = { it.id }) { rec ->
                    RecordRow(
                        rec = rec,
                        mask = rec.effectivePrivacy,
                        hasAccounts = state.hasAccounts,
                        onClick = { onRecordClick(rec) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            item {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("—— 到底了 ——", fontSize = 14.sp, color = c.text3)
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    year: Int,
    month: Int,
    isOnCurrentMonth: Boolean,
    onMonthClick: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onBackToCurrent: () -> Unit,
) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickableNoRipple(onMonthClick),
        ) {
            Text(
                "$year 年 $month 月",
                color = c.text,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
            )
            HSpace(8.dp)
            Icon(LineIcons.ChevD, null, tint = c.text3, modifier = Modifier.size(14.dp))
        }
        AnimatedVisibility(visible = !isOnCurrentMonth) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HSpace(12.dp)
                Box(
                    Modifier
                        .background(c.subtle, RoundedCornerShape(999.dp))
                        .clickableNoRipple(onBackToCurrent)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("回本月", color = c.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Box(Modifier.weight(1f))
        Icon(
            LineIcons.ChevL,
            null,
            tint = c.text2,
            modifier = Modifier.size(18.dp).clickableNoRipple(onPrev),
        )
        HSpace(14.dp)
        Icon(
            LineIcons.ChevR,
            null,
            tint = c.text2,
            modifier = Modifier.size(18.dp).clickableNoRipple(onNext),
        )
    }
}

@Composable
private fun BoardWarm(
    expenseCents: Long,
    incomeCents: Long,
    balanceCents: Long,
    maskExpense: Boolean,
    maskIncome: Boolean,
    maskBalance: Boolean,
    dailyExpense: List<Long>,
    todayIndex: Int,
) {
    val c = LocalAppColors.current
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.surface, RoundedCornerShape(16.dp))
                .border(1.dp, c.hairline, RoundedCornerShape(16.dp))
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("本月支出", color = c.text2, fontSize = 14.sp)
                HSpace(8.dp)
                Box(Modifier.size(3.dp).background(c.accent, CircleShape))
            }
            VSpace(2.dp)
            Text(
                text = if (maskExpense) "¥•••••" else "¥" + formatYuan(expenseCents),
                color = c.text,
                fontSize = 46.sp,
                fontWeight = FontWeight.Medium,
            )
            VSpace(14.dp)
            MiniBarChart(daily = dailyExpense, todayIndex = todayIndex)
            VSpace(14.dp)
            Hairline()
            VSpace(14.dp)
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("收入", color = c.text2, fontSize = 13.sp)
                    VSpace(3.dp)
                    Text(
                        text = if (maskIncome) "¥•••" else "¥" + formatYuan(incomeCents),
                        color = c.income,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("结余", color = c.text2, fontSize = 13.sp)
                    VSpace(3.dp)
                    Text(
                        text = if (maskBalance) "¥•••" else "¥" + formatYuan(balanceCents),
                        color = c.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniBarChart(daily: List<Long>, todayIndex: Int) {
    val c = LocalAppColors.current
    if (daily.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(26.dp))
        return
    }
    val max = daily.max().coerceAtLeast(1L)
    Row(
        Modifier.fillMaxWidth().height(26.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        daily.forEachIndexed { i, cents ->
            // 最小条高 2dp 以保持节奏感（即使当天没支出也显示一条底边）
            val h = if (cents <= 0L) 2f else (26f * (cents.toFloat() / max.toFloat())).coerceAtLeast(3f)
            val isToday = i == todayIndex
            Box(
                Modifier
                    .weight(1f)
                    .height(h.dp)
                    .background(
                        color = when {
                            isToday -> c.accent
                            cents > 0L -> c.text3
                            else -> c.subtle
                        },
                        shape = RoundedCornerShape(1.dp),
                    ),
            )
        }
    }
}

@Composable
private fun DayHeader(day: DayGroup) {
    val c = LocalAppColors.current
    val monthNum = day.date.monthNumber
    val dayNum = day.date.dayOfMonth
    val weekday = when (day.date.dayOfWeek.name) {
        "MONDAY" -> "周一"
        "TUESDAY" -> "周二"
        "WEDNESDAY" -> "周三"
        "THURSDAY" -> "周四"
        "FRIDAY" -> "周五"
        "SATURDAY" -> "周六"
        "SUNDAY" -> "周日"
        else -> ""
    }
    val dayExp = day.expenseCentsSum
    Column(Modifier.fillMaxWidth().background(c.bg)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "$monthNum 月 $dayNum 日",
                color = c.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            HSpace(10.dp)
            Text(
                text = weekday,
                color = c.text3,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 2.dp),
            )
            Box(Modifier.weight(1f))
            Text(
                text = "支出 ¥" + formatYuan(dayExp),
                color = c.text2,
                fontSize = 13.sp,
            )
        }
        Hairline()
    }
}

@Composable
private fun LazyItemScope.RecordRow(
    rec: RecordDetail,
    mask: Boolean,
    hasAccounts: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalAppColors.current
    val isIncome = rec.kind == RecordKind.Income
    val isPrivate = rec.effectivePrivacy
    val amountStr = when {
        mask || isPrivate -> "••••"
        else -> (if (isIncome) "+" else "−") + formatYuan(rec.amountCents)
    }
    val timeStr = run {
        val dt = rec.occurredAt.toLocalDateTime(com.chaos.bin.mt.data.AppTimeZone)
        val hh = dt.hour.toString().padStart(2, '0')
        val mm = dt.minute.toString().padStart(2, '0')
        "$hh:$mm"
    }
    Row(
        modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiChip(emoji = rec.categoryEmoji, colors = c)
        HSpace(12.dp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rec.categoryName, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (!rec.subCategoryName.isNullOrBlank()) {
                    Text(" · ${rec.subCategoryName}", color = c.text2, fontSize = 15.sp)
                }
                if (isPrivate) {
                    HSpace(5.dp)
                    Icon(LineIcons.Lock, null, tint = c.text3, modifier = Modifier.size(11.dp))
                }
            }
            VSpace(2.dp)
            Text(
                text = buildString {
                    append(timeStr)
                    if (rec.note.isNotBlank()) append(" · ").append(rec.note)
                    if (hasAccounts) append(" · ").append(rec.accountName)
                },
                color = c.text2,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HSpace(8.dp)
        Text(
            text = amountStr,
            color = if (isIncome) c.income else c.expense,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** 金额格式化：以"分"为单位的 Long → "12,345.67" 字符串。 */
fun formatYuan(cents: Long): String {
    val negative = cents < 0
    val abs = kotlin.math.abs(cents)
    val yuan = abs / 100
    val c = (abs % 100).toInt()
    val yuanStr = formatThousands(yuan)
    val centStr = c.toString().padStart(2, '0')
    return (if (negative) "-" else "") + "$yuanStr.$centStr"
}

internal fun formatThousands(n: Long): String {
    val s = n.toString()
    val sb = StringBuilder()
    val rem = s.length % 3
    s.forEachIndexed { i, ch ->
        if (i != 0 && (i - rem) % 3 == 0) sb.append(',')
        sb.append(ch)
    }
    return sb.toString()
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick,
)
