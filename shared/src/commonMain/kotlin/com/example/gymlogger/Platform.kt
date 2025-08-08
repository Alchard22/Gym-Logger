package com.example.gymlogger

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform