package com.chaos.bin.mt.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.components.HSpace
import com.chaos.bin.mt.ui.components.Hairline
import com.chaos.bin.mt.ui.components.LineIcons
import com.chaos.bin.mt.ui.components.PageHeader
import com.chaos.bin.mt.ui.components.ThemedSwitch
import com.chaos.bin.mt.ui.components.VSpace

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState()),
    ) {
        PageHeader(title = "隐私保护", onBack = onBack)

        Text(
            "遮蔽敏感金额和分类，保护你在公共场合的隐私。",
            color = c.text2,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        )

        Group(title = "看板显示") {
            Row2(title = "默认显示金额", desc = "关闭后看板显示 ***") { ThemedSwitch(on = true) }
            Row2(title = "进入 App 时遮蔽", desc = "每次打开自动隐藏金额") { ThemedSwitch(on = false) }
            Row2(title = "打码样式") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("¥ •••••", color = c.text2, fontSize = 14.sp)
                    HSpace(4.dp)
                    Icon(LineIcons.ChevR, null, tint = c.text2, modifier = Modifier.size(12.dp))
                }
            }
        }

        Group(
            title = "已标记隐私的分类",
            desc = "在首页列表中按隐私方式显示，金额会被遮蔽",
        ) {
            val items = listOf(
                Triple("医疗 · 药品", "大类+小类", true),
                Triple("医疗 · 门诊", "小类", true),
                Triple("人情 · 送礼", "小类", true),
                Triple("人情 · 红包", "小类", true),
                Triple("理财 · 利息", "小类", true),
                Triple("理财 · 分红", "小类", true),
            )
            items.forEach { (n, sub, on) ->
                Row2(
                    titleContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(LineIcons.Lock, null, tint = c.text3, modifier = Modifier.size(11.dp))
                            HSpace(5.dp)
                            Text(n, color = c.text, fontSize = 15.sp)
                        }
                    },
                    desc = sub,
                ) { ThemedSwitch(on = on) }
            }
        }

        Group(title = "其他") {
            Row2(title = "应用锁", desc = "Face ID / 指纹") { ThemedSwitch(on = false) }
            Row2(title = "截屏时遮蔽", desc = "防止录屏/投屏时泄露") { ThemedSwitch(on = true) }
        }

        VSpace(20.dp)
    }
}

@Composable
private fun Group(
    title: String,
    desc: String? = null,
    content: @Composable () -> Unit,
) {
    val c = LocalAppColors.current
    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Text(
            title,
            color = c.text3,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.surface),
        ) {
            Hairline()
            content()
            Hairline()
        }
        if (desc != null) {
            Text(
                desc,
                color = c.text3,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
            )
        }
    }
}

@Composable
private fun Row2(
    title: String,
    desc: String? = null,
    right: @Composable () -> Unit,
) {
    Row2(
        titleContent = {
            val c = LocalAppColors.current
            Text(title, color = c.text, fontSize = 15.sp)
        },
        desc = desc,
        right = right,
    )
}

@Composable
private fun Row2(
    titleContent: @Composable () -> Unit,
    desc: String? = null,
    right: @Composable () -> Unit,
) {
    val c = LocalAppColors.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                titleContent()
                if (desc != null) {
                    VSpace(2.dp)
                    Text(desc, color = c.text3, fontSize = 13.sp)
                }
            }
            HSpace(10.dp)
            right()
        }
        Hairline()
    }
}
