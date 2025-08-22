package com.example.gymlogger.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(GymDatabase.Schema, DatabaseConfig.GYM_DATABASE_NAME)
    }
}