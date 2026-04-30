package com.chaos.bin.mt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chaos.bin.mt.theme.LocalAppColors

/** 备注 / 名称 等简单文本编辑对话框。 */
@Composable
fun TextEditDialog(
    title: String,
    initial: String,
    placeholder: String = "",
    singleLine: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalAppColors.current
    var text by rememberSaveable(initial) { mutableStateOf(initial) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val submit: () -> Unit = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onConfirm(text)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.surface, RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Text(title, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            VSpace(14.dp)

            Box(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, c.line, RoundedCornerShape(8.dp))
                    .background(c.bg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .defaultMinSize(minHeight = if (singleLine) 36.dp else 80.dp),
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = singleLine,
                    keyboardOptions = if (singleLine) {
                        KeyboardOptions(imeAction = ImeAction.Done)
                    } else {
                        KeyboardOptions.Default
                    },
                    keyboardActions = if (singleLine) {
                        KeyboardActions(onDone = { submit() })
                    } else {
                        KeyboardActions.Default
                    },
                    textStyle = TextStyle(color = c.text, fontSize = 16.sp),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (text.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, color = c.text3, fontSize = 16.sp)
                }
            }

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
                ) {
                    Text("取消", color = c.text2, fontSize = 16.sp)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .background(c.accent, RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                onConfirm(text)
                                onDismiss()
                            },
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "确定",
                        color = c.accentText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
