package com.chaos.bin.mt.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaos.bin.mt.data.AutoRule
import com.chaos.bin.mt.data.RecordKind
import com.chaos.bin.mt.data.TriggerConfig
import com.chaos.bin.mt.di.LocalAppContainer
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.components.HSpace
import com.chaos.bin.mt.ui.components.LineIcons
import com.chaos.bin.mt.ui.components.PageHeader
import com.chaos.bin.mt.ui.components.ThemedSwitch
import com.chaos.bin.mt.ui.components.VSpace
import com.chaos.bin.mt.ui.home.formatThousands

@Composable
fun AutomationScreen(
    onBack: () -> Unit,
    onEdit: (Long?) -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: AutomationListViewModel = viewModel { AutomationListViewModel(container) }
    val rules by vm.rules.collectAsStateWithLifecycle()

    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState()),
    ) {
        PageHeader(
            title = "自动记账",
            onBack = onBack,
            right = {
                Box(
                    Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onEdit(null) },
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text("新建", color = c.accent, fontSize = 15.sp)
                }
            },
        )

        Text(
            "按时间规则自动生成记录，适合工资、房租、订阅等固定支出收入。",
            color = c.text2,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
        )

        if (rules.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("还没有规则，点击右上角 新建 添加", color = c.text3, fontSize = 14.sp)
            }
        } else {
            Column(Modifier.padding(horizontal = 16.dp)) {
                rules.forEach { r ->
                    RuleCard(
                        rule = r,
                        onClick = { onEdit(r.id) },
                        onToggle = { vm.setEnabled(r.id, it) },
                    )
                    VSpace(8.dp)
                }
            }
        }
        VSpace(24.dp)
    }
}

@Composable
private fun RuleCard(
    rule: AutoRule,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(12.dp))
            .border(1.dp, c.hairline, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .alpha(if (rule.enabled) 1f else 0.55f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(rule.name.ifBlank { "（未命名）" }, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    HSpace(6.dp)
                    Box(
                        Modifier
                            .background(c.chip, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            if (rule.kind == RecordKind.Expense) "支出" else "收入",
                            color = c.text2,
                            fontSize = 12.sp,
                        )
                    }
                }
                VSpace(4.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(LineIcons.Repeat, null, tint = c.text2, modifier = Modifier.size(10.dp))
                    HSpace(4.dp)
                    Text(describeTrigger(rule.trigger), color = c.text2, fontSize = 13.sp)
                }
            }
            ThemedSwitch(on = rule.enabled, onChange = onToggle)
        }
        VSpace(10.dp)
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
        VSpace(10.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (rule.note.isBlank()) "—" else rule.note,
                color = c.text3,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = (if (rule.kind == RecordKind.Income) "+" else "−") + "¥" + formatThousands(rule.amountCents / 100),
                color = if (rule.kind == RecordKind.Income) c.income else c.expense,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private val WeekdayLabels = listOf("日", "一", "二", "三", "四", "五", "六")

internal fun describeTrigger(t: TriggerConfig): String {
    val hh = t.hour.toString().padStart(2, '0')
    val mm = t.minute.toString().padStart(2, '0')
    val time = "$hh:$mm"
    return when (t) {
        is TriggerConfig.Weekly -> {
            val days = (0..6).filter { (t.weekdaysMask and (1 shl it)) != 0 }
                .joinToString("·") { WeekdayLabels[it] }
            if (days.isEmpty()) "每周 $time" else "每周 $days $time"
        }
        is TriggerConfig.MonthlyDays -> {
            val days = (0..30).filter { (t.daysMask and (1 shl it)) != 0 }
                .joinToString("·") { (it + 1).toString() }
            if (days.isEmpty()) "每月 $time" else "每月 $days 号 $time"
        }
        is TriggerConfig.Interval -> "每隔 ${t.intervalDays} 天 $time"
    }
}
