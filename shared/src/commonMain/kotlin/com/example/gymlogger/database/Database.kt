package com.example.gymlogger.database

import app.cash.sqldelight.db.SqlDriver
import database.GymDatabaseQueries

class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val driver: SqlDriver = databaseDriverFactory.createDriver()
    internal val database: GymDatabase = GymDatabase(driver)

    internal val dbQuery: GymDatabaseQueries = database.gymDatabaseQueries
}