package io.github.stream29

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel

@Suppress("unused")
@LLMDescription("Tools to communicate with user")
class CommunicationTools(val eventBus: SendChannel<AgentEvent>) : ToolSet {
    @Tool
    @LLMDescription("Wait for user input. This suspends execution until the user provides input.")
    suspend fun waitForUserInput(): String {
        val input = CompletableDeferred<String>()
        eventBus.send(AgentEvent.WaitForUserInput(input))
        return input.await()
    }

    @Tool
    @LLMDescription("Say something to the user. Use this to communicate with the user.")
    suspend fun sayToUser(
        @LLMDescription("The message to say to the user")
        message: String
    ): String {
        eventBus.send(AgentEvent.SayToUser(message))
        return "Message sent to user successfully."
    }
}