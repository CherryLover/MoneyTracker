package com.chaos.bin.mt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.chaos.bin.mt.data.RecordDetail
import com.chaos.bin.mt.data.RecordKind
import com.chaos.bin.mt.theme.LocalAppColors
import kotlinx.datetime.toLocalDateTime

/**
 * 点记录行后的操作面板：显示详情 + 删除按钮。
 * 编辑功能留到二期（需要改 nav 结构或做独立编辑面板）。
 */
@Composable
fun RecordActionsDialog(
    record: RecordDetail,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalAppColors.current
    var confirmDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.surface, RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            // 头部：emoji + 分类名 + 金额
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiChip(emoji = record.categoryEmoji)
                HSpace(12.dp)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            record.categoryName,
                            color = c.text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (!record.subCategoryName.isNullOrBlank()) {
                            Text(" · ${record.subCategoryName}", color = c.text2, fontSize = 16.sp)
                        }
                    }
                    VSpace(2.dp)
                    val dt = record.occurredAt.toLocalDateTime(AppTimeZone)
                    Text(
                        "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}-${dt.dayOfMonth.toString().padStart(2, '0')} " +
                            "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}",
                        color = c.text3,
                        fontSize = 14.sp,
                    )
                }
                val isIncome = record.kind == RecordKind.Income
                Text(
                    text = (if (isIncome) "+" else "−") + "¥" + formatYuanForDialog(record.amountCents),
                    color = if (isIncome) c.income else c.expense,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            VSpace(14.dp)
            Hairline()
            VSpace(12.dp)

            // 详情行
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailLine("账户", record.accountName)
                if (record.note.isNotBlank()) {
                    DetailLine("备注", record.note)
                }
                if (record.effectivePrivacy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(LineIcons.Lock, null, tint = c.text3, modifier = Modifier.size(12.dp))
                        HSpace(4.dp)
                        Text("隐私条目", color = c.text3, fontSize = 13.sp)
                    }
                }
            }

            VSpace(18.dp)

            // 操作按钮
            if (!confirmDelete) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogButton(
                        text = "编辑",
                        bg = c.accent,
                        fg = c.accentText,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onEdit,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DialogButton(
                            text = "关闭",
                            bg = c.subtle,
                            fg = c.text2,
                            modifier = Modifier.weight(1f),
                            onClick = onDismiss,
                        )
                        DialogButton(
                            text = "删除",
                            bg = c.subtle,
                            fg = c.expense,
                            modifier = Modifier.weight(1f),
                            onClick = { confirmDelete = true },
                        )
                    }
                }
            } else {
                Text(
                    "删除后无法恢复，确认？",
                    color = c.text2,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DialogButton(
                        text = "取消",
                        bg = c.subtle,
                        fg = c.text2,
                        modifier = Modifier.weight(1f),
                        onClick = { confirmDelete = false },
                    )
                    DialogButton(
                        text = "确认删除",
                        bg = c.accent,
                        fg = c.accentText,
                        modifier = Modifier.weight(1f),
                        onClick = onDelete,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    val c = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = c.text3, fontSize = 14.sp, modifier = Modifier.padding(end = 12.dp))
        Text(value, color = c.text, fontSize = 15.sp)
    }
}

@Composable
private fun DialogButton(
    text: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatYuanForDialog(cents: Long): String {
    val abs = kotlin.math.abs(cents)
    val yuan = abs / 100
    val c = (abs % 100).toInt()
    val yuanStr = yuan.toString().let { s ->
        val sb = StringBuilder()
        if (cents < 0) sb.append('-')
        val rem = s.length % 3
        s.forEachIndexed { i, ch ->
            if (i != 0 && (i - rem) % 3 == 0) sb.append(',')
            sb.append(ch)
        }
        sb.toString()
    }
    val centStr = c.toString().padStart(2, '0')
    return "$yuanStr.$centStr"
}
