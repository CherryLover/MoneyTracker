package com.chaos.bin.mt.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaos.bin.mt.theme.AppColors
import com.chaos.bin.mt.theme.LocalAppColors

/** 水平分隔线 */
@Composable
fun Hairline(color: Color = LocalAppColors.current.hairline) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))

/** 页面头部：返回箭头 + 标题 + 可选右侧 */
@Composable
fun PageHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    right: @Composable (() -> Unit)? = null,
) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = LineIcons.Back,
            contentDescription = null,
            tint = c.text,
            modifier = Modifier.size(20.dp).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onBack?.invoke() },
            ),
        )
        HSpace(10.dp)
        Text(
            text = title,
            color = c.text,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (right != null) right()
    }
}

/** 受控 Switch（静态渲染，含动画） */
@Composable
fun ThemedSwitch(on: Boolean, onChange: ((Boolean) -> Unit)? = null) {
    val c = LocalAppColors.current
    val track by animateColorAsState(if (on) c.accent else c.line)
    val offset by animateDpAsState(if (on) 16.dp else 2.dp)
    Box(
        Modifier
            .size(width = 34.dp, height = 20.dp)
            .background(track, shape = RoundedCornerShape(10.dp))
            .then(
                if (onChange != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onChange(!on) },
                ) else Modifier,
            ),
    ) {
        Box(
            Modifier
                .padding(top = 2.dp, start = offset)
                .size(16.dp)
                .background(Color.White, CircleShape),
        )
    }
}

/** Pill 类型切换（支出 / 收入） */
@Composable
fun TypeToggle(
    current: String,
    options: List<Pair<String, String>>, // key -> label
    onChange: (String) -> Unit,
) {
    val c = LocalAppColors.current
    Row(
        modifier = Modifier
            .background(c.subtle, RoundedCornerShape(999.dp))
            .border(1.dp, c.hairline, RoundedCornerShape(999.dp))
            .padding(3.dp),
    ) {
        options.forEach { (k, label) ->
            val active = current == k
            Box(
                modifier = Modifier
                    .background(
                        color = if (active) c.accent else Color.Transparent,
                        shape = RoundedCornerShape(999.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onChange(k) },
                    )
                    .padding(horizontal = 18.dp, vertical = 6.dp),
            ) {
                Text(
                    text = label,
                    color = if (active) c.accentText else c.text2,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** 条目中的一行输入字段（时间/账户/备注） */
@Composable
fun FieldLine(
    icon: ImageVector,
    label: String,
    placeholder: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.text3, modifier = Modifier.size(15.dp))
        HSpace(8.dp)
        Text(
            text = label,
            color = if (placeholder) c.text3 else c.text2,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(LineIcons.ChevR, null, tint = c.text3, modifier = Modifier.size(13.dp))
    }
}

/** 圆形 emoji 背景的小图标（列表行里的） */
@Composable
fun EmojiChip(emoji: String, size: Dp = 36.dp, colors: AppColors = LocalAppColors.current) {
    Box(
        Modifier
            .size(size)
            .background(colors.chip, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 19.sp, textAlign = TextAlign.Center)
    }
}
