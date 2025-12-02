package io.github.stream29.vibekoog

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val agent = createAgent()
    
    println("Koog Coding Agent (TUI)")
    println("-----------------------")
    print("You: ")
    val initialInput = readlnOrNull() ?: return@runBlocking
    
    try {
        val result = agent.run(initialInput)
        println("Agent finished.")
        println("Final Result: $result")
    } catch (e: Exception) {
        println("Agent encountered an error: ${e.message}")
        e.printStackTrace()
    }
}