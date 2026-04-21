package com.chaos.bin.mt.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.chaos.bin.mt.theme.LocalAppColors
import com.chaos.bin.mt.ui.components.Hairline
import com.chaos.bin.mt.ui.components.LineIcons
import com.chaos.bin.mt.ui.entry.EntryScreen
import com.chaos.bin.mt.ui.home.HomeScreen
import com.chaos.bin.mt.ui.settings.AccountsScreen
import com.chaos.bin.mt.ui.settings.AutomationScreen
import com.chaos.bin.mt.ui.settings.CategoriesScreen
import com.chaos.bin.mt.ui.settings.PrivacyScreen
import com.chaos.bin.mt.ui.settings.SettingsDest
import com.chaos.bin.mt.ui.settings.SettingsHomeScreen

@Composable
fun MainScaffold() {
    val c = LocalAppColors.current
    TabNavigator(HomeTab) {
        Column(
            Modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                CurrentTab()
            }
            Hairline()
            BottomTabBar()
        }
    }
}

@Composable
private fun BottomTabBar() {
    val c = LocalAppColors.current
    val nav = LocalTabNavigator.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(68.dp),
    ) {
        listOf(HomeTab, EntryTab, SettingsTab).forEach { tab ->
            val active = nav.current.key == tab.key
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { nav.current = tab },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val opts = tab.options
                val painter = opts.icon
                if (painter != null) {
                    // material3 Icon 接受 painter
                    androidx.compose.material3.Icon(
                        painter = painter,
                        contentDescription = opts.title,
                        tint = if (active) c.text else c.text3,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = opts.title,
                    color = if (active) c.text else c.text3,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

object HomeTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 0u,
            title = "首页",
            icon = rememberVectorPainter(LineIcons.Home),
        )

    @Composable
    override fun Content() {
        HomeScreen()
    }
}

object EntryTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = "记一笔",
            icon = rememberVectorPainter(LineIcons.Plus),
        )

    @Composable
    override fun Content() {
        EntryScreen()
    }
}

object SettingsTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 2u,
            title = "设置",
            icon = rememberVectorPainter(LineIcons.Cog),
        )

    @Composable
    override fun Content() {
        Navigator(SettingsHomeScreenRoute)
    }
}

private object SettingsHomeScreenRoute : Screen {
    @Composable
    override fun Content() {
        val nav = LocalNavigator.currentOrThrow
        SettingsHomeScreen(
            onOpen = { dest ->
                when (dest) {
                    SettingsDest.Accounts -> nav.push(AccountsScreenRoute)
                    SettingsDest.Categories -> nav.push(CategoriesScreenRoute)
                    SettingsDest.Automation -> nav.push(AutomationScreenRoute)
                    SettingsDest.Privacy -> nav.push(PrivacyScreenRoute)
                }
            },
        )
    }
}

private object AccountsScreenRoute : Screen {
    @Composable
    override fun Content() {
        val nav = LocalNavigator.currentOrThrow
        AccountsScreen(onBack = { nav.pop() })
    }
}

private object CategoriesScreenRoute : Screen {
    @Composable
    override fun Content() {
        val nav = LocalNavigator.currentOrThrow
        CategoriesScreen(onBack = { nav.pop() })
    }
}

private object AutomationScreenRoute : Screen {
    @Composable
    override fun Content() {
        val nav = LocalNavigator.currentOrThrow
        AutomationScreen(onBack = { nav.pop() })
    }
}

private object PrivacyScreenRoute : Screen {
    @Composable
    override fun Content() {
        val nav = LocalNavigator.currentOrThrow
        PrivacyScreen(onBack = { nav.pop() })
    }
}
