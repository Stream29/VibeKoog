package io.github.stream29

import java.io.File
import java.util.*

object Config {
    private val env = System.getenv()
    private val localPropsFile = File("local.properties")
    private val props = Properties().apply { load(localPropsFile.inputStream()) }
    private val apiKeyName = "ANTHROPIC_API_KEY"
    val apiKey =
        props.getProperty(apiKeyName) ?: env[apiKeyName] ?: throw IllegalStateException("ANTHROPIC_API_KEY not found")
}