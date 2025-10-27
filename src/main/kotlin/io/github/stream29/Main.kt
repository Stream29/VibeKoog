package io.github.stream29

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor

suspend fun main() {
    hackSlf4j()
    println("=".repeat(60))
    println("Coding Agent with Koog Framework")
    println("=".repeat(60))
    println()
    val apiKey = Config.apiKey
    val toolRegistry = ToolRegistry {
        tools(FileTools())
        tools(CommunicationTools())
        tools(KotlinScriptTools())
    }

    val agent = AIAgent(
        promptExecutor = simpleAnthropicExecutor(apiKey),
        strategy = codingAgentStrategy,
        agentConfig = agentConfig,
        toolRegistry = toolRegistry
    ) {
        handleEvents {
            onToolCallCompleted { toolCall ->
                val toolName = toolCall.tool.name
                if (toolName == "sayToUser" || toolName == "waitForUserInput") return@onToolCallCompleted
                println("[Tool Call] $toolName")
                println("[Tool Call Args] ${toolCall.toolArgs}")
                println("[Tool Call Output] ${toolCall.result}")
            }
        }
    }
    print("[User]: ")
    val userInput = readln()

    agent.run(userInput)
}
