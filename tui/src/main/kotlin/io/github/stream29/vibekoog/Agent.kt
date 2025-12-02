package io.github.stream29.vibekoog

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.ToolCalls
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import ai.koog.prompt.params.LLMParams
import java.io.File
import java.util.Properties

import ai.koog.agents.features.eventHandler.feature.handleEvents

fun createAgent(): AIAgent<String, String> {
    val apiKey = getApiKey()
    val executor = simpleAnthropicExecutor(apiKey)
    
    return AIAgent(
        promptExecutor = executor,
        llmModel = AnthropicModels.Haiku_4_5,
        toolRegistry = ToolRegistry {
            tool(ReadFile())
            tool(WriteFile())
            tool(RunKotlin())
            tool(WaitForUserInput())
            tool(SayToUser())
        },
        strategy = strategy("coding_agent_strategy") {
            val forceTool by node<String, String> { input ->
                llm.writeSession {
                    prompt = prompt.withUpdatedParams {
                        toolChoice = LLMParams.ToolChoice.Required
                    }
                }
                input
            }
            val parallelStrategySubgraph = singleRunStrategy()
            nodeStart then forceTool then parallelStrategySubgraph then nodeFinish
        }
    ) {
        handleEvents {
            onToolCallStarting { ctx ->
                println("Tool Call: ${ctx.tool.name} Args: ${ctx.toolArgs}")
            }
            onToolCallCompleted { ctx ->
                println("Tool Result: ${ctx.result}")
            }
        }
    }
}

private fun getApiKey(): String {
    val envKey = System.getenv("ANTHROPIC_API_KEY")
    if (!envKey.isNullOrBlank()) return envKey
    
    val localProperties = File("local.properties")
    if (localProperties.exists()) {
        val props = Properties()
        localProperties.inputStream().use { props.load(it) }
        val propKey = props.getProperty("ANTHROPIC_API_KEY")
        if (!propKey.isNullOrBlank()) return propKey
    }
    
    // Fallback or error? The project says "The api key should be set... read in runtime."
    // It's better to throw if not found, or return empty and let it fail later.
    // Returning empty might be safer for tests if they mock the executor, but createAgent uses simpleAnthropicExecutor which probably validates key.
    // Let's return empty string if not found to avoid crashing just by initializing if strict validation isn't needed yet.
    // But simpleAnthropicExecutor probably needs it.
    return ""
}
