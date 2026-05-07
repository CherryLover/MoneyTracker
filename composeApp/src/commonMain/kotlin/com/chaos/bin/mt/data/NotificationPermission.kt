package com.chaos.bin.mt.data

expect class NotificationPermission {
    suspend fun isGranted(): Boolean
    suspend fun request(): Boolean
    fun openAppSettings()
}
