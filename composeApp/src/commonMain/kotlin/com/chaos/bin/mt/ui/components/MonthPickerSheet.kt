package com.chaos.bin.mt.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaos.bin.mt.data.MonthSummary
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.home.formatYuan

/**
 * 底部月份选择弹窗。半屏/全屏可拖拽。按年分组，每行显示月份 + 支出 + 收入。
 *
 * @param summaries 每月已有的汇总（空行显示为 0）
 * @param selectedYear 当前首页选中年
 * @param selectedMonth 当前首页选中月
 * @param currentYear 今天所在年（用于"本月"标记）
 * @param currentMonth 今天所在月
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MonthPickerSheet(
    summaries: Map<Pair<Int, Int>, MonthSummary>,
    selectedYear: Int,
    selectedMonth: Int,
    currentYear: Int,
    currentMonth: Int,
    onPick: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // 当前年前后各 50 年
    val yearSpan = 50
    val startYear = currentYear - yearSpan
    val endYear = currentYear + yearSpan
    val totalRows = (endYear - startYear + 1) * 12

    // 初始滚动到「今天所在月」。注意：LazyColumn 里年份是倒序排列，sticky header 也要算进偏移；
    // 按"今年 12 月 → 今年 1 月 → 上一年 12 月..."的行顺序估算即可。
    val initialIndex = run {
        // 目标所在的"从最新年（endYear）12 月起的累计行数"
        // 年份越大越靠上；每年：header + 12 行（月份倒序 12→1）
        var rows = 0
        var reached = 0
        for (year in endYear downTo startYear) {
            rows += 1 // sticky header
            for (month in 12 downTo 1) {
                if (year == currentYear && month == currentMonth) {
                    reached = rows
                }
                rows += 1
            }
        }
        reached
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (initialIndex - 2).coerceAtLeast(0),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        contentColor = c.text,
    ) {
        Column(Modifier.fillMaxSize()) {
            // 顶部标题
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("选择月份", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Box(Modifier.weight(1f))
                Box(
                    Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                onPick(currentYear, currentMonth)
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("回到本月", color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            Hairline()

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                for (year in endYear downTo startYear) {
                    stickyHeader {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(c.subtle)
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            Text(
                                "$year 年",
                                color = c.text2,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    for (month in 12 downTo 1) {
                        item(key = "m-$year-$month") {
                            val summary = summaries[year to month] ?: MonthSummary(0, 0)
                            val isCurrent = year == currentYear && month == currentMonth
                            val isSelected = year == selectedYear && month == selectedMonth
                            MonthRow(
                                year = year,
                                month = month,
                                summary = summary,
                                isCurrent = isCurrent,
                                isSelected = isSelected,
                                onClick = { onPick(year, month) },
                            )
                            Hairline()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthRow(
    year: Int,
    month: Int,
    summary: MonthSummary,
    isCurrent: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (isSelected) c.accent.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                "$month 月",
                color = if (isSelected) c.accent else c.text,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            )
            if (isCurrent) {
                HSpace(8.dp)
                Box(
                    Modifier
                        .background(c.accent, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                ) {
                    Text("本月", color = c.accentText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "支 ¥" + formatYuan(summary.expenseCents),
                color = c.expense,
                fontSize = 13.sp,
            )
            VSpace(2.dp)
            Text(
                "收 ¥" + formatYuan(summary.incomeCents),
                color = c.income,
                fontSize = 13.sp,
            )
        }
    }
}

