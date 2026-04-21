package com.chaos.bin.mt.db

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseFactory(private val context: Context) {
    actual fun create(): MtDatabase {
        val driver = AndroidSqliteDriver(
            schema = MtDatabase.Schema,
            context = context.applicationContext,
            name = "moneytracker.db",
        )
        return MtDatabase(driver)
    }
}
