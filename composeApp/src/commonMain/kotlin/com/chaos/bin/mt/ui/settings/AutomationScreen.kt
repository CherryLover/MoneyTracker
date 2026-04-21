package com.chaos.bin.mt.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaos.bin.mt.data.AutoRule
import com.chaos.bin.mt.data.MockData
import com.chaos.bin.mt.data.RecordType
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.components.HSpace
import com.chaos.bin.mt.ui.components.LineIcons
import com.chaos.bin.mt.ui.components.PageHeader
import com.chaos.bin.mt.ui.components.ThemedSwitch
import com.chaos.bin.mt.ui.components.VSpace
import com.chaos.bin.mt.ui.home.formatThousands

@Composable
fun AutomationScreen(onBack: () -> Unit) {
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
                Text("新建", color = c.accent, fontSize = 15.sp)
            },
        )

        Text(
            "按时间规则自动生成记录，适合工资、房租、订阅等固定支出收入。",
            color = c.text2,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
        )

        Column(Modifier.padding(horizontal = 16.dp)) {
            MockData.autoRules.forEach { r ->
                RuleCard(r)
                VSpace(8.dp)
            }
        }
        VSpace(12.dp)

        Text(
            "支持的触发规则",
            color = c.text3,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 20.dp, top = 6.dp, bottom = 4.dp),
        )
        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RuleTypeRow(LineIcons.Repeat, "每周定时", "每周一 / 周二 ... 指定时间")
            RuleTypeRow(LineIcons.Cal, "每月某天", "每月 1 号、15 号、最后一天...")
            RuleTypeRow(LineIcons.Cal, "每月第几个周几", "每月第 2 个周日")
            RuleTypeRow(LineIcons.Clock, "精确到时分", "可预设账户、分类、金额、备注")
        }
        VSpace(24.dp)
    }
}

@Composable
private fun RuleCard(r: AutoRule) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(12.dp))
            .border(1.dp, c.hairline, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .alpha(if (r.enabled) 1f else 0.55f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.name, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    HSpace(6.dp)
                    Box(
                        Modifier
                            .background(c.chip, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            if (r.type == RecordType.Expense) "支出" else "收入",
                            color = c.text2,
                            fontSize = 12.sp,
                        )
                    }
                }
                VSpace(4.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(LineIcons.Repeat, null, tint = c.text2, modifier = Modifier.size(10.dp))
                    HSpace(4.dp)
                    Text(r.rule, color = c.text2, fontSize = 13.sp)
                }
            }
            ThemedSwitch(on = r.enabled)
        }
        VSpace(10.dp)
        // 虚线分割线（用一条实线近似）
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
        VSpace(10.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${r.cat} · ${r.account}", color = c.text3, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(
                text = (if (r.type == RecordType.Income) "+" else "−") + "¥" + formatThousands(r.amount.toLong()),
                color = if (r.type == RecordType.Income) c.income else c.expense,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun RuleTypeRow(icon: ImageVector, title: String, desc: String) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(10.dp))
            .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.text2, modifier = Modifier.size(15.dp))
        HSpace(10.dp)
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            VSpace(1.dp)
            Text(desc, color = c.text3, fontSize = 13.sp)
        }
    }
}
