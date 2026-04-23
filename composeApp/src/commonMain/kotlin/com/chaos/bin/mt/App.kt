package com.chaos.bin.mt

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.chaos.bin.mt.di.AppContainer
import com.chaos.bin.mt.di.LocalAppContainer
import com.chaos.bin.mt.theme.AppTheme
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.nav.MainScaffold

@Composable
@Preview
fun App(container: AppContainer) {
    val dark = isSystemInDarkTheme()

    LaunchedEffect(container) {
        container.seeder.seedIfEmpty()
        container.autoRuleScheduler.catchUp()
    }

    CompositionLocalProvider(LocalAppContainer provides container) {
        AppTheme(dark = dark) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = LocalAppColors.current.bg,
            ) {
                MainScaffold()
            }
        }
    }
}
