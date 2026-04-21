package com.chaos.bin.mt

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.chaos.bin.mt.db.DatabaseFactory
import com.chaos.bin.mt.di.AppContainer

fun MainViewController() = ComposeUIViewController {
    val container = remember { AppContainer(DatabaseFactory().create()) }
    App(container)
}
