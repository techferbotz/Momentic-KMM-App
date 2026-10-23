package com.ferbotz.momentic

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform