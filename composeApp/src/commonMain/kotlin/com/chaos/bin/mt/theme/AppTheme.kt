package com.chaos.bin.mt.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 对应 theme.jsx 的 warm 变体，使用 light / dark 两套配色。 */
@Immutable
data class AppColors(
    val bg: Color,
    val surface: Color,
    val subtle: Color,
    val line: Color,
    val hairline: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val accentText: Color,
    val expense: Color,
    val income: Color,
    val chip: Color,
    val chipText: Color,
    val isDark: Boolean,
)

val WarmLight = AppColors(
    bg = Color(0xFFF5F1EA),
    surface = Color(0xFFFBF8F2),
    subtle = Color(0xFFEFEADF),
    line = Color(0xFFE3DCCF),
    hairline = Color(0xFFEBE5D9),
    text = Color(0xFF2A2620),
    text2 = Color(0xFF857E71),
    text3 = Color(0xFFB4AD9F),
    accent = Color(0xFFC7623F),
    accentText = Color(0xFFFBF8F2),
    expense = Color(0xFF2A2620),
    income = Color(0xFF6B7C5A),
    chip = Color(0xFFECE6D8),
    chipText = Color(0xFF2A2620),
    isDark = false,
)

val WarmDark = AppColors(
    bg = Color(0xFF1A1814),
    surface = Color(0xFF211E18),
    subtle = Color(0xFF26231C),
    line = Color(0xFF322E26),
    hairline = Color(0xFF2A2620),
    text = Color(0xFFECE6D8),
    text2 = Color(0xFF9C9486),
    text3 = Color(0xFF5C564A),
    accent = Color(0xFFE08162),
    accentText = Color(0xFF1A1814),
    expense = Color(0xFFECE6D8),
    income = Color(0xFF9CB087),
    chip = Color(0xFF2A2620),
    chipText = Color(0xFFECE6D8),
    isDark = true,
)

val LocalAppColors = staticCompositionLocalOf<AppColors> { WarmLight }

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) WarmDark else WarmLight
    CompositionLocalProvider(LocalAppColors provides colors, content = content)
}
