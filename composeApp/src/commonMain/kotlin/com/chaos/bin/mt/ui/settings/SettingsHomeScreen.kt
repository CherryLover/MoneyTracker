package com.chaos.bin.mt.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaos.bin.mt.data.CsvImportService
import com.chaos.bin.mt.data.formatCsv
import com.chaos.bin.mt.data.nowInstant
import com.chaos.bin.mt.data.rememberCsvFileAccess
import com.chaos.bin.mt.di.LocalAppContainer
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.components.HSpace
import com.chaos.bin.mt.ui.components.Hairline
import com.chaos.bin.mt.ui.components.LineIcons
import com.chaos.bin.mt.ui.components.VSpace
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class SettingsDest { Categories, Automation, Privacy, Accounts }

@Composable
fun SettingsHomeScreen(onOpen: (SettingsDest) -> Unit) {
    val c = LocalAppColors.current
    val container = LocalAppContainer.current
    val csvAccess = rememberCsvFileAccess()
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        // 标题
        Text(
            "设置",
            color = c.text,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        )

        NavGroup(
            items = listOf(
                Triple(LineIcons.Wallet, "账户管理", SettingsDest.Accounts) to "管理收支使用的账户",
                Triple(LineIcons.Cog, "分类管理", SettingsDest.Categories) to "支出 / 收入 分类与小类",
                Triple(LineIcons.Repeat, "自动记账", SettingsDest.Automation) to "定时规则自动生成记录",
                Triple(LineIcons.Lock, "隐私保护", SettingsDest.Privacy) to "遮蔽金额、应用锁、截屏隐藏",
            ),
            onOpen = onOpen,
        )

        VSpace(20.dp)

        ActionGroup(
            items = listOf(
                Triple(LineIcons.Cal, "导出 CSV", "把所有记账导出为 CSV 文件"),
                Triple(LineIcons.Repeat, "导入 CSV", "从 CSV 文件批量导入记账"),
            ),
            enabled = !busy,
            onClick = { index ->
                if (busy) return@ActionGroup
                if (index == 0) {
                    scope.launch {
                        busy = true
                        try {
                            val records = container.recordRepository.getAll()
                            val content = formatCsv(records)
                            val name = "moneytracker-${todayStamp()}.csv"
                            val ok = csvAccess.exportToUserFile(name, content)
                            dialogMessage = if (ok) "已导出 ${records.size} 条记录" else "导出已取消或失败"
                        } finally {
                            busy = false
                        }
                    }
                } else {
                    scope.launch {
                        busy = true
                        try {
                            val content = csvAccess.importFromUserFile()
                            if (content == null) {
                                dialogMessage = "导入已取消"
                            } else {
                                val service = CsvImportService(
                                    container.categoryRepository,
                                    container.accountRepository,
                                    container.recordRepository,
                                )
                                val result = service.importCsv(content)
                                dialogMessage = buildString {
                                    append("导入成功 ${result.insertedCount} 条")
                                    if (result.skippedCount > 0) {
                                        append("，跳过 ${result.skippedCount} 条")
                                    }
                                }
                            }
                        } finally {
                            busy = false
                        }
                    }
                }
            },
        )
    }

    dialogMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { dialogMessage = null },
            title = { Text("提示") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { dialogMessage = null }) { Text("好") }
            },
        )
    }
}

private fun todayStamp(): String {
    val ldt = nowInstant().toLocalDateTime(TimeZone.currentSystemDefault())
    val y = ldt.year.toString().padStart(4, '0')
    val mo = ldt.monthNumber.toString().padStart(2, '0')
    val d = ldt.dayOfMonth.toString().padStart(2, '0')
    return "$y$mo$d"
}

@Composable
private fun NavGroup(
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
        items.forEachIndexed { _, (meta, desc) ->
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
                    Text(title, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    VSpace(2.dp)
                    Text(desc, color = c.text3, fontSize = 13.sp)
                }
                Icon(LineIcons.ChevR, null, tint = c.text3, modifier = Modifier.size(14.dp))
            }
            Hairline()
        }
    }
}

@Composable
private fun ActionGroup(
    items: List<Triple<ImageVector, String, String>>,
    enabled: Boolean,
    onClick: (Int) -> Unit,
) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.surface),
    ) {
        Hairline()
        items.forEachIndexed { index, (icon, title, desc) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onClick(index) },
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = c.text2, modifier = Modifier.size(18.dp))
                HSpace(12.dp)
                Column(Modifier.weight(1f)) {
                    Text(title, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    VSpace(2.dp)
                    Text(desc, color = c.text3, fontSize = 13.sp)
                }
                Icon(LineIcons.ChevR, null, tint = c.text3, modifier = Modifier.size(14.dp))
            }
            Hairline()
        }
    }
}
