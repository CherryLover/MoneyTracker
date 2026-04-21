package com.chaos.bin.mt.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.components.HSpace
import com.chaos.bin.mt.ui.components.Hairline
import com.chaos.bin.mt.ui.components.LineIcons
import com.chaos.bin.mt.ui.components.VSpace

enum class SettingsDest { Categories, Automation, Privacy }

@Composable
fun SettingsHomeScreen(onOpen: (SettingsDest) -> Unit) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        // 标题
        Text(
            "设置",
            color = c.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        )

        Group(
            items = listOf(
                Triple(LineIcons.Cog, "分类管理", SettingsDest.Categories) to "支出 / 收入 分类与小类",
                Triple(LineIcons.Repeat, "自动记账", SettingsDest.Automation) to "定时规则自动生成记录",
                Triple(LineIcons.Lock, "隐私保护", SettingsDest.Privacy) to "遮蔽金额、应用锁、截屏隐藏",
            ),
            onOpen = onOpen,
        )
    }
}

@Composable
private fun Group(
    items: List<Pair<Triple<ImageVector, String, SettingsDest>, String>>,
    onOpen: (SettingsDest) -> Unit,
) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.surface),
    ) {
        Hairline()
        items.forEachIndexed { index, (meta, desc) ->
            val (icon, title, dest) = meta
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onOpen(dest) },
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = c.text2, modifier = Modifier.size(18.dp))
                HSpace(12.dp)
                Column(Modifier.weight(1f)) {
                    Text(title, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    VSpace(2.dp)
                    Text(desc, color = c.text3, fontSize = 11.5.sp)
                }
                Icon(LineIcons.ChevR, null, tint = c.text3, modifier = Modifier.size(14.dp))
            }
            Hairline()
        }
    }
}
