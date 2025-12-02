# Coding Agent with Koog

## References

### Koog

Koog is an AI agent framework built by JetBrains.

You can read its source code at `reference/koog`.

You can read its examples at `reference/koog/examples`.

### SimpleMainKts

SimpleMainKts is an example of using Kotlin MainKts scripting.

You can read its source code at `reference/SimpleMainKts/app/src/main/kotlin/io/github/stream29/simplemainkts/app/App.kt`.

## Core Logic

This project uses the Koog framework to build a coding agent.

This project uses Anthropic `claude-haiku-4-5` model. (`AnthropicModels.Haiku_4_5`)
The api key should be set in `local.properties` or environment variables and named `ANTHROPIC_API_KEY`, read in runtime. 

## Agent Strategy

We need a strategy that always only produces and processes tool calls.
This must be done by the following strategy: 

```kotlin
import ai.koog.agents.core.agent.ToolCalls
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.prompt.params.LLMParams

strategy("coding_agent_strategy") {
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
```

You mustn't use any other strategy than this.

### Tools

The agent has those main tools:

#### ReadFile

`ReadFile` to read files. Three parameters: `path`, `from_line`, `to_line`. The two last parameters are optional.
If the content of the file is too big, return with a warning that the content is too big and `from_line` and `to_line` should be used to limit the size.

#### WriteFile

`WriteFile` to write files. Four parameters: `path` , `original_content`, `edited_content`, `replace_all`.
If `replace_all` is true, replace all the `original_content` of the file with `edited_content`.
If `replace_all` is false and `original_content` occurs multiple times in the file, do nothing and return a warning.

#### RunKotlin

`RunKotlin` to run MainKts Kotlin scripts. Two parameters: `script` and `output`.
`script` is the content of the script.
If `output` is `console`, capture the console output of the script and return it to the agent.
If `output` is `return`, return the returned value of the script to the agent.
You should declare the usage of the `RunKotlin` tool in the description.
A MainKts script can use Maven dependencies like `@file:DependsOn("<coordinates>")`.
A MainKts script does not need a `main` function.
The last line of the script is considered as the return value of the script.

#### WaitForUserInput

`WaitForUserInput` to stop working and wait for the user to input a prompt.
This tool should be a suspend function that suspends until the user inputs a prompt.
(This requires calling `suspendCoroutine` and store the `Continuation` in the agent)
The user input should be returned to the agent.

#### SayToUser

`SayToUser` to say something to the user.

You shouldn't use `toTextSerializer`. Use the original serializer of the result.

## Interface

The agent should use `println` and `readln` with `kotlinx.coroutines` to build a non-blocking TUI.
Users can input prompts to the agent.
The agent should be able to have a multi-turn conversation.
The first input from the user is passed to the parameter of the agent.
Then user can only input after `WaitForUserInput`.
The tool calls and its result should be logged in the console.

## Development

You must write tests for your code and make them all pass.

You must keep your code concise. Make it an MVP.

Dependencies are already included in `build.gradle.kts`.

You should read the examples of Koog before you define agent, tools and strategies.