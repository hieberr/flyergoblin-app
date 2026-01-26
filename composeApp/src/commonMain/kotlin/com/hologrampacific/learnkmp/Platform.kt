package com.hologrampacific.learnkmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform