package io.github.stream29

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("unused")
@LLMDescription("Tools to communicate with user")
class CommunicationTools() : ToolSet {
    @Tool
    @LLMDescription("Wait for user input. This suspends execution until the user provides input.")
    suspend fun waitForUserInput(): String = withContext(Dispatchers.IO) {
        print("[User]: ")
        readln()
    }

    @Tool
    @LLMDescription("Say something to the user. Use this to communicate with the user.")
    fun sayToUser(
        @LLMDescription("The message to say to the user")
        message: String
    ): String {
        println("[Agent]: $message")
        return "Message sent to user successfully."
    }
}