package com.example.gymlogger.android

import android.app.Application
import com.example.gymlogger.database.DatabaseDriverFactory
import com.example.gymlogger.database.DatabaseModule

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        DatabaseModule.initialize(DatabaseDriverFactory(this))
    }
}