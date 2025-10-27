package io.github.stream29

fun hackSlf4j() {
    val loggerFactory = org.slf4j.LoggerFactory::class.java
    loggerFactory.getDeclaredField("INITIALIZATION_STATE").apply {
        isAccessible = true
        setInt(null, 4)
    }
}