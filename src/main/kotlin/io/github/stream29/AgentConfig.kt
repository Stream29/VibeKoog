package io.github.stream29

import ai.koog.agents.core.agent.ToolCalls
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.params.LLMParams


val codingAgentStrategy = strategy<String, String>("coding_agent_strategy") {
    val forceTool by node<String, String> { input ->
        llm.writeSession {
            prompt = prompt.withUpdatedParams {
                toolChoice = LLMParams.ToolChoice.Required
            }
        }
        input
    }
    val parallelStrategySubgraph = singleRunStrategy(ToolCalls.PARALLEL)
    nodeStart then forceTool then parallelStrategySubgraph then nodeFinish
}

val agentConfig = AIAgentConfig(
    prompt = prompt("coding_agent") {
        system(
            """
                You are a coding assistant that helps users with software development tasks.
                You have access to tools to read files, write files, execute Kotlin scripts, and interact with users.

                When you need to read or modify files, use the appropriate tools.
                When you need to run code or perform computations, use the runKotlin tool.
                When you need to communicate with the user, use the sayToUser tool.
                When you need additional information from the user, use the waitForUserInput tool.
                
                You should always try to call tools parallelly to decrease latency.

                Always be helpful and precise in your responses.
            """.trimIndent()
        )
    },
    model = AnthropicModels.Sonnet_4_5,
    maxAgentIterations = 100
)