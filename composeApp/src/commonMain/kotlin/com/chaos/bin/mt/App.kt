package com.chaos.bin.mt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import com.chaos.bin.mt.theme.AppTheme
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.nav.MainScaffold

@Composable
@Preview
fun App() {
    val dark = isSystemInDarkTheme()
    AppTheme(dark = dark) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LocalAppColors.current.bg,
        ) {
            MainScaffold()
        }
    }
}
