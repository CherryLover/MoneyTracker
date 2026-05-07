package com.chaos.bin.mt.data

actual class NotificationPermission {
    actual suspend fun isGranted(): Boolean = false
    actual suspend fun request(): Boolean = false
    actual fun openAppSettings() = Unit
}
