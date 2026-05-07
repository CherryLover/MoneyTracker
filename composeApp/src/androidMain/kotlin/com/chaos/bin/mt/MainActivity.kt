package com.chaos.bin.mt

import android.Manifest
import android.os.Bundle
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.chaos.bin.mt.data.NotificationPermission
import com.chaos.bin.mt.data.NotificationScheduler
import com.chaos.bin.mt.db.DatabaseFactory
import com.chaos.bin.mt.di.AppContainer
import com.chaos.bin.mt.notif.ensureReminderNotificationChannel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ensureReminderNotificationChannel(applicationContext)
        val notificationPermission = NotificationPermission(applicationContext)
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            notificationPermission.onRequestResult(granted)
        }
        notificationPermission.requestLauncher = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                notificationPermission.onRequestResult(true)
            }
        }
        val container = AppContainer(
            database = DatabaseFactory(applicationContext).create(),
            notificationScheduler = NotificationScheduler(applicationContext),
            notificationPermission = notificationPermission,
        )
        setContent {
            App(container)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    // 预览里不接真实 DB
}
