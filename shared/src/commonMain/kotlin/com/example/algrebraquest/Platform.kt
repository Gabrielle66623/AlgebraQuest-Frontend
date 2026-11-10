package com.example.algrebraquest

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform