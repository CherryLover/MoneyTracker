package com.chaos.bin.mt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaos.bin.mt.theme.LocalAppColors

@Composable
fun TimeStepper(hour: Int, minute: Int, onHour: (Int) -> Unit, onMinute: (Int) -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeColumn(
            label = "时",
            value = hour,
            maxExclusive = 24,
            onChange = onHour,
        )
        HSpace(10.dp)
        Text(":", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        HSpace(10.dp)
        TimeColumn(
            label = "分",
            value = minute,
            maxExclusive = 60,
            onChange = onMinute,
        )
    }
}

@Composable
private fun RowScope.TimeColumn(
    label: String,
    value: Int,
    maxExclusive: Int,
    onChange: (Int) -> Unit,
) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .weight(1f)
            .background(c.surface, RoundedCornerShape(10.dp))
            .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeStepperButton("−") { onChange(((value - 1) + maxExclusive) % maxExclusive) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                value.toString().padStart(2, '0'),
                color = c.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        TimeStepperButton("+") { onChange((value + 1) % maxExclusive) }
    }
}

@Composable
private fun TimeStepperButton(sign: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        Modifier
            .size(34.dp)
            .background(c.subtle, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(sign, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}
