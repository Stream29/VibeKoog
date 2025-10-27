package io.github.stream29

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun main() {
    hackSlf4j()
    println("=".repeat(60))
    println("Coding Agent with Koog Framework")
    println("=".repeat(60))
    println()
    val apiKey = Config.apiKey
    val eventBus = Channel<AgentEvent>(Channel.BUFFERED)
    val toolRegistry = ToolRegistry {
        tools(FileTools(eventBus))
        tools(CommunicationTools(eventBus))
        tools(KotlinScriptTools(eventBus))
    }
    val agent = AIAgent(
        promptExecutor = simpleAnthropicExecutor(apiKey),
        strategy = codingAgentStrategy,
        agentConfig = agentConfig,
        toolRegistry = toolRegistry
    )
    withContext(Dispatchers.IO) {
        launch {
            while (true) {
                eventBus.consumeEach { it.handle() }
            }
        }
        launch {
            val input = CompletableDeferred<String>()
            eventBus.send(AgentEvent.WaitForUserInput(input))
            agent.run(input.await())
        }
    }
}
