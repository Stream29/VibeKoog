package io.github.stream29.vibekoog

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.jetbrains.kotlin.mainKts.MainKtsScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlinx.serialization.builtins.serializer

class ReadFile : Tool<ReadFile.Args, String>() {
    @Serializable
    data class Args(
        @LLMDescription("The path to the file")
        val path: String,
        @LLMDescription("The first line to read (1-based, inclusive). Optional.")
        val from_line: Int? = null,
        @LLMDescription("The last line to read (1-based, inclusive). Optional.")
        val to_line: Int? = null
    )

    override val name = "ReadFile"
    override val description = """
        Read files. Three parameters: path, from_line, to_line. The two last parameters are optional.
        If the content of the file is too big, return with a warning that the content is too big and from_line and to_line should be used to limit the size.
    """.trimIndent()

    override val argsSerializer = Args.serializer()
    override val resultSerializer = String.serializer()

    override suspend fun execute(args: Args): String {
        val file = File(args.path)
        if (!file.exists()) return "Error: File not found at ${args.path}"
        if (!file.isFile) return "Error: ${args.path} is not a file"

        val lines = file.readLines()
        if (args.from_line == null && args.to_line == null && lines.size > 1000) { // Threshold for "too big"
            return "Warning: The content of the file is too big (${lines.size} lines). Please use from_line and to_line to limit the size."
        }

        val fromIndex = (args.from_line?.minus(1))?.coerceAtLeast(0) ?: 0
        val toIndex = (args.to_line?.minus(1))?.coerceAtMost(lines.size - 1) ?: (lines.size - 1)
        
        if (fromIndex > toIndex) return "Error: from_line must be less than or equal to to_line"

        return lines.subList(fromIndex, toIndex + 1).joinToString("\n")
    }
}

class WriteFile : Tool<WriteFile.Args, String>() {
    @Serializable
    data class Args(
        @LLMDescription("The path to the file")
        val path: String,
        @LLMDescription("The original content to be replaced. Required if replace_all is false.")
        val original_content: String? = null,
        @LLMDescription("The new content")
        val edited_content: String,
        @LLMDescription("Whether to replace the entire file content")
        val replace_all: Boolean
    )

    override val name = "WriteFile"
    override val description = """
        Write files. Four parameters: path , original_content, edited_content, replace_all.
        If replace_all is true, replace all the original_content of the file with edited_content.
        If replace_all is false and original_content occurs multiple times in the file, do nothing and return a warning.
    """.trimIndent()

    override val argsSerializer = Args.serializer()
    override val resultSerializer = String.serializer()

    override suspend fun execute(args: Args): String {
        val file = File(args.path)
        
        if (args.replace_all) {
            file.parentFile?.mkdirs()
            file.writeText(args.edited_content)
            return "File written successfully."
        }

        if (!file.exists()) return "Error: File not found at ${args.path}"
        
        val content = file.readText()
        val original = args.original_content ?: return "Error: original_content is required when replace_all is false"
        
        // Count occurrences
        val occurrences = content.windowed(original.length).count { it == original }
        
        if (occurrences == 0) return "Error: original_content not found in file."
        if (occurrences > 1) return "Warning: original_content occurs $occurrences times in the file. Please provide more context or use replace_all=true."
        
        val newContent = content.replace(original, args.edited_content)
        file.writeText(newContent)
        return "File updated successfully."
    }
}

class RunKotlin : Tool<RunKotlin.Args, String>() {
    @Serializable
    data class Args(
        @LLMDescription("The Kotlin script content")
        val script: String,
        @LLMDescription("Output mode: 'console' or 'return'")
        val output: String
    )

    override val name = "RunKotlin"
    override val description = """
        Run MainKts Kotlin scripts. Two parameters: script and output.
        script is the content of the script.
        If output is console, capture the console output of the script and return it to the agent.
        If output is return, return the returned value of the script to the agent.
        A MainKts script can use Maven dependencies like @file:DependsOn("<coordinates>").
        A MainKts script does not need a main function.
        The last line of the script is considered as the return value of the script.
    """.trimIndent()

    override val argsSerializer = Args.serializer()
    override val resultSerializer = String.serializer()

    private val host = BasicJvmScriptingHost()

    override suspend fun execute(args: Args): String {
        // Capture console output
        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream)
        val originalOut = System.out
        val originalErr = System.err
        
        try {
            if (args.output == "console") {
                System.setOut(printStream)
                System.setErr(printStream)
            }

            val result = host.evalWithTemplate<MainKtsScript>(
                script = args.script.toScriptSource(),
                evaluation = {
                    constructorArgs(emptyArray<String>())
                }
            )

            if (args.output == "console") {
                System.out.flush()
                System.err.flush()
                return outputStream.toString()
            } else {
                val returnValue = result.valueOrNull()?.returnValue
                return when (returnValue) {
                    is ResultValue.Value -> returnValue.value.toString()
                    is ResultValue.Unit -> "Unit"
                    is ResultValue.Error -> "Error: ${returnValue.error}"
                    is ResultValue.NotEvaluated -> "NotEvaluated"
                    else -> returnValue.toString()
                }
            }
        } catch (e: Exception) {
             return "Exception during execution: ${e.message}"
        } finally {
            if (args.output == "console") {
                System.setOut(originalOut)
                System.setErr(originalErr)
            }
        }
    }
}

class WaitForUserInput : Tool<WaitForUserInput.Args, String>() {
    @Serializable
    data class Args(
        @LLMDescription("Optional prompt/reason for waiting")
        val prompt: String? = null
    )
    
    override val name = "WaitForUserInput"
    override val description = "Stop working and wait for the user to input a prompt. The user input should be returned to the agent."
    
    override val argsSerializer = Args.serializer()
    override val resultSerializer = String.serializer()

    override suspend fun execute(args: Args): String {
        if (args.prompt != null) {
            println(args.prompt)
        }
        // In a real TUI, we might need to signal that we are waiting.
        // Since we use println/readln, we just readln.
        return withContext(Dispatchers.IO) {
            readlnOrNull() ?: ""
        }
    }
}

class SayToUser : Tool<SayToUser.Args, String>() {
    @Serializable
    data class Args(
        @LLMDescription("The message to say to the user")
        val message: String
    )
    
    override val name = "SayToUser"
    override val description = "Say something to the user."
    
    override val argsSerializer = Args.serializer()
    override val resultSerializer = String.serializer()

    override suspend fun execute(args: Args): String {
        println(args.message)
        return "Message sent."
    }
}
