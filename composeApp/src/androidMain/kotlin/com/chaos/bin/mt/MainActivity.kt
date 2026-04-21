package com.chaos.bin.mt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.chaos.bin.mt.db.DatabaseFactory
import com.chaos.bin.mt.di.AppContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = AppContainer(DatabaseFactory(applicationContext).create())
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
