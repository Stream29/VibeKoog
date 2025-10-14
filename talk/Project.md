# Coding Agent with Koog

## References

### Koog

Koog is an AI agent framework built by JetBrains.

You can read its source code and readme at `reference/koog`.

You can read its example at `reference/koog/examples`.

### SimpleMainKts

SimpleMainKts is an example of using Kotlin MainKts scripting.

You can read its source code and readme at `reference/SimpleMainKts`.

### Mosaic

Mosaic is a compose-styled TUI framework built by JakeWharton.

You can read its source code and readme at `reference/mosaic`.

You can read its example at `reference/mosaic/samples`.

## Core Logic

This project uses the Koog framework to build a coding agent.

This project uses Anthropic `claude-sonnet-4-5-20250929` model.
The api key should be set in `local.properties` or environment variables and named `ANTHROPIC_API_KEY`, read in runtime. 

## Agent Strategy

The default strategy of a Koog agent is single-run.
We need to change the strategy to not ending and chat with tools.
You should define an agent strategy graph to implement that.
(Agent loops with calling tools)

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
This tool should update the view model to append a message to the user.

## Interface

The agent should use `JakeWharton/mosaic` to build a TUI.
The TUI should be interactive, always showing the status of the agent.
Users can input prompts to the agent through a chatbox.
The status update of the agent should be shown correctly.
(Store the events in a `List` in the view model and show them!)
The agent should be able to have a multi-turn conversation.
The tool calls and its result should be recorded as events and shown in the TUI.

## Development

You must write tests for your code and make them all pass.

You must keep your code concise. Make it an MVP.