package com.chaos.bin.mt.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

actual class NotificationPermission(private val context: Context) {
    private val appContext = context.applicationContext

    @Volatile
    private var pendingContinuation: ((Boolean) -> Unit)? = null

    var requestLauncher: (() -> Unit)? = null

    actual suspend fun isGranted(): Boolean {
        val notificationEnabled = NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return notificationEnabled
        return notificationEnabled && ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    actual suspend fun request(): Boolean {
        if (isGranted()) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        }
        return suspendCancellableCoroutine { cont ->
            pendingContinuation = { granted -> cont.resume(granted) }
            cont.invokeOnCancellation { pendingContinuation = null }
            val launcher = requestLauncher
            if (launcher == null) {
                pendingContinuation = null
                cont.resume(false)
            } else {
                launcher.invoke()
            }
        }
    }

    fun onRequestResult(granted: Boolean) {
        pendingContinuation?.invoke(granted)
        pendingContinuation = null
    }

    actual fun openAppSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData("package:${appContext.packageName}".toUri())
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }
}
