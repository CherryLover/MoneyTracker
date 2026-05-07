package com.chaos.bin.mt

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.chaos.bin.mt.data.NotificationPermission
import com.chaos.bin.mt.data.NotificationScheduler
import com.chaos.bin.mt.db.DatabaseFactory
import com.chaos.bin.mt.di.AppContainer

fun main() = application {
    val container = AppContainer(
        database = DatabaseFactory().create(),
        notificationScheduler = NotificationScheduler(),
        notificationPermission = NotificationPermission(),
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "Mt",
    ) {
        App(container)
    }
}
