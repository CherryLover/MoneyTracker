package com.chaos.bin.mt.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaos.bin.mt.data.AppTimeZone
import com.chaos.bin.mt.theme.LocalAppColors
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private enum class Field { Year, Month, Day, Hour, Minute }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerSheet(
    initial: Instant,
    onConfirm: (Instant) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initLdt = initial.toLocalDateTime(AppTimeZone)

    var year by remember { mutableStateOf(initLdt.year) }
    var month by remember { mutableStateOf(initLdt.monthNumber) }
    var day by remember { mutableStateOf(initLdt.dayOfMonth) }
    var hour by remember { mutableStateOf(initLdt.hour) }
    var minute by remember { mutableStateOf(initLdt.minute) }

    // 当月天数变化时 clamp day
    val maxDay = daysInMonth(year, month)
    if (day > maxDay) day = maxDay

    // 哪一列滚轮当前是"展开的"（蒙版隐藏）。null = 全部用蒙版盖住。
    var expanded by remember { mutableStateOf<Field?>(null) }

    val yearRange = (initLdt.year - 10)..(initLdt.year + 10)
    val years = remember { yearRange.map { it.toString() } }
    val months = remember { (1..12).map { it.toString().padStart(2, '0') } }
    val days = remember(year, month) { (1..maxDay).map { it.toString().padStart(2, '0') } }
    val hours = remember { (0..23).map { it.toString().padStart(2, '0') } }
    val minutes = remember { (0..59).map { it.toString().padStart(2, '0') } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        contentColor = c.text,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            // 顶部工具栏
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        )
                        .padding(8.dp),
                ) { Text("取消", color = c.text2, fontSize = 16.sp) }
                Box(Modifier.weight(1f))
                Box(
                    Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                val ldt = LocalDateTime(
                                    year = year,
                                    monthNumber = month,
                                    dayOfMonth = day,
                                    hour = hour,
                                    minute = minute,
                                    second = 0,
                                    nanosecond = 0,
                                )
                                onConfirm(ldt.toInstant(AppTimeZone))
                            },
                        )
                        .padding(8.dp),
                ) {
                    Text("确定", color = c.accent, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
            Hairline()

            // 五列滚轮，默认都被蒙版盖住（chip 样式）；点击某列 → 该列蒙版淡出显示滚轮
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FieldWheel(
                    items = years,
                    selectedIndex = (year - yearRange.first).coerceIn(0, years.size - 1),
                    expanded = expanded == Field.Year,
                    onTap = { expanded = if (expanded == Field.Year) null else Field.Year },
                    onSelected = { year = yearRange.first + it },
                    weight = 1.4f,
                )
                Separator("-")
                FieldWheel(
                    items = months,
                    selectedIndex = month - 1,
                    expanded = expanded == Field.Month,
                    onTap = { expanded = if (expanded == Field.Month) null else Field.Month },
                    onSelected = { month = it + 1 },
                )
                Separator("-")
                FieldWheel(
                    items = days,
                    selectedIndex = (day - 1).coerceIn(0, days.size - 1),
                    expanded = expanded == Field.Day,
                    onTap = { expanded = if (expanded == Field.Day) null else Field.Day },
                    onSelected = { day = it + 1 },
                )
                HSpace(8.dp)
                FieldWheel(
                    items = hours,
                    selectedIndex = hour,
                    expanded = expanded == Field.Hour,
                    onTap = { expanded = if (expanded == Field.Hour) null else Field.Hour },
                    onSelected = { hour = it },
                )
                Separator(":")
                FieldWheel(
                    items = minutes,
                    selectedIndex = minute,
                    expanded = expanded == Field.Minute,
                    onTap = { expanded = if (expanded == Field.Minute) null else Field.Minute },
                    onSelected = { minute = it },
                )
            }
        }
    }
}

@Composable
private fun RowScope.FieldWheel(
    items: List<String>,
    selectedIndex: Int,
    expanded: Boolean,
    onTap: () -> Unit,
    onSelected: (Int) -> Unit,
    weight: Float = 1f,
) {
    val c = LocalAppColors.current
    val itemHeight = 36.dp
    val visibleCount = 5
    val padding = visibleCount / 2
    val wheelHeight = itemHeight * visibleCount
    val maskBandHeight = itemHeight * padding

    val overlayAlpha by animateFloatAsState(
        targetValue = if (expanded) 0f else 1f,
        label = "fieldOverlay",
    )

    Box(
        Modifier
            .weight(weight)
            .height(wheelHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            ),
    ) {
        WheelPicker(
            items = items,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            itemHeight = itemHeight,
            visibleCount = visibleCount,
        )
        // 蒙版：上下两条与 sheet 同色的遮挡，中间 itemHeight 高的空档露出选中项
        if (overlayAlpha > 0.01f) {
            Column(Modifier.fillMaxSize().alpha(overlayAlpha)) {
                Box(Modifier.fillMaxWidth().height(maskBandHeight).background(c.surface))
                Box(Modifier.fillMaxWidth().height(itemHeight))
                Box(Modifier.fillMaxWidth().height(maskBandHeight).background(c.surface))
            }
        }
    }
}

@Composable
private fun Separator(s: String) {
    val c = LocalAppColors.current
    Text(
        s,
        color = c.text3,
        fontSize = 22.sp,
        fontWeight = FontWeight.Light,
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    else -> 30
}
