package com.chaos.bin.mt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chaos.bin.mt.theme.LocalAppColors

/** 通用的「从列表里挑一个」弹窗：emoji + 名称，支持当前选中高亮。 */
@Composable
fun <T : Any> PickerDialog(
    title: String,
    items: List<T>,
    selectedKey: String?,
    keyOf: (T) -> String,
    emojiOf: (T) -> String,
    labelOf: (T) -> String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalAppColors.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.surface, RoundedCornerShape(16.dp))
                .padding(vertical = 12.dp),
        ) {
            Text(
                title,
                color = c.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Hairline()
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(items) { item ->
                    val active = keyOf(item) == selectedKey
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    onPick(item)
                                    onDismiss()
                                },
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .background(c.chip, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(emojiOf(item), fontSize = 18.sp)
                        }
                        HSpace(12.dp)
                        Text(
                            labelOf(item),
                            color = if (active) c.accent else c.text,
                            fontSize = 16.sp,
                            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (active) {
                            Icon(
                                LineIcons.Check,
                                null,
                                tint = c.accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            Hairline()
            Box(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("取消", color = c.text2, fontSize = 16.sp)
            }
        }
    }
}
