package com.chaos.bin.mt.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

actual class DatabaseFactory {
    actual fun create(): MtDatabase {
        val dir = File(System.getProperty("user.home"), ".moneytracker").apply {
            if (!exists()) mkdirs()
        }
        val dbFile = File(dir, "moneytracker.db")
        val isNew = !dbFile.exists()
        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            properties = Properties(),
        )
        if (isNew) {
            MtDatabase.Schema.create(driver)
        }
        return MtDatabase(driver)
    }
}
