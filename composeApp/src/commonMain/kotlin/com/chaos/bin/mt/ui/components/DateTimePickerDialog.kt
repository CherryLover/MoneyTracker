package com.chaos.bin.mt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chaos.bin.mt.data.AppTimeZone
import com.chaos.bin.mt.theme.LocalAppColors
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * 日期 + 时间选择器。
 * 设计上同时展示日、月简易切换和时分 +/- 按钮，避免引入 Material3 DatePicker 的重型依赖。
 */
@Composable
fun DateTimePickerDialog(
    initial: Instant,
    onConfirm: (Instant) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalAppColors.current
    val initLdt = initial.toLocalDateTime(AppTimeZone)
    var year by remember { mutableStateOf(initLdt.year) }
    var month by remember { mutableStateOf(initLdt.monthNumber) }
    var day by remember { mutableStateOf(initLdt.dayOfMonth) }
    var hour by remember { mutableStateOf(initLdt.hour) }
    var minute by remember { mutableStateOf(initLdt.minute) }

    fun clampDay() {
        val maxDay = daysInMonth(year, month)
        if (day > maxDay) day = maxDay
        if (day < 1) day = 1
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.surface, RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Text("选择日期时间", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            VSpace(16.dp)

            // 日期：年 / 月 / 日
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StepperRow(
                    label = "年",
                    value = year.toString(),
                    onDec = { year--; clampDay() },
                    onInc = { year++; clampDay() },
                )
                StepperRow(
                    label = "月",
                    value = month.toString(),
                    onDec = {
                        if (month == 1) { month = 12; year-- } else month--
                        clampDay()
                    },
                    onInc = {
                        if (month == 12) { month = 1; year++ } else month++
                        clampDay()
                    },
                )
                StepperRow(
                    label = "日",
                    value = day.toString(),
                    onDec = {
                        val max = daysInMonth(year, month)
                        if (day == 1) { day = max } else day--
                    },
                    onInc = {
                        val max = daysInMonth(year, month)
                        if (day == max) { day = 1 } else day++
                    },
                )
            }

            VSpace(18.dp)
            Hairline()
            VSpace(12.dp)

            // 时间：时 / 分（分钟 ± 5）
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StepperRow(
                    label = "时",
                    value = hour.toString().padStart(2, '0'),
                    onDec = { hour = (hour + 23) % 24 },
                    onInc = { hour = (hour + 1) % 24 },
                )
                StepperRow(
                    label = "分",
                    value = minute.toString().padStart(2, '0'),
                    onDec = { minute = (minute + 55) % 60 },
                    onInc = { minute = (minute + 5) % 60 },
                )
            }

            VSpace(8.dp)
            Text(
                "当前：$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')} " +
                    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
                color = c.text2,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp),
            )

            VSpace(16.dp)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .background(c.subtle, RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("取消", color = c.text2, fontSize = 16.sp) }
                Box(
                    Modifier
                        .weight(1f)
                        .background(c.accent, RoundedCornerShape(8.dp))
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
                                val instant = ldt.toInstant(AppTimeZone)
                                onConfirm(instant)
                                onDismiss()
                            },
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("确定", color = c.accentText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onDec: () -> Unit,
    onInc: () -> Unit,
) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = c.text2, fontSize = 15.sp, modifier = Modifier.width(32.dp))
        HSpace(8.dp)
        StepperButton(text = "−", onClick = onDec)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(value, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
        StepperButton(text = "+", onClick = onInc)
    }
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        Modifier
            .size(34.dp)
            .background(c.subtle, CircleShape)
            .border(1.dp, c.hairline, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = c.text, fontSize = 21.sp, fontWeight = FontWeight.Medium)
    }
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    else -> 30
}
