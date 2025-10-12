package com.example.gymlogger

object AppSettings{
    private const val simpleDateFormat = "dd MMM y"

    val getDateFormat: String
        get() = simpleDateFormat
}