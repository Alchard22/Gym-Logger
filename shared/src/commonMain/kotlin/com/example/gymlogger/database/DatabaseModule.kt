package com.example.gymlogger.database

import com.example.gymlogger.repository.GymRepository

object DatabaseModule {
    private var database: Database? = null
    private var repository: GymRepository? = null

    fun initialize(databaseDriverFactory: DatabaseDriverFactory) {
        database = Database(databaseDriverFactory)
        repository = GymRepository(database!!.database)
    }

    fun getRepository(): GymRepository {
        return repository ?: throw IllegalStateException("DatabaseModule not initialised")
    }
}