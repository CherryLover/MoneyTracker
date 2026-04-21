package com.chaos.bin.mt.db

import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseFactory {
    actual fun create(): MtDatabase {
        val driver = NativeSqliteDriver(
            schema = MtDatabase.Schema,
            name = "moneytracker.db",
        )
        return MtDatabase(driver)
    }
}
