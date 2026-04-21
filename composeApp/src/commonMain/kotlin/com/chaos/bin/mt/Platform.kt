package com.chaos.bin.mt

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform