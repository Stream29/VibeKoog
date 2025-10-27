package io.github.stream29

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.coroutines.channels.SendChannel
import java.io.File

@Suppress("unused")
@LLMDescription("Tools to write or read files.")
class FileTools(val eventBus: SendChannel<AgentEvent>) : ToolSet {
    @Tool
    @LLMDescription("Read the content of a file. If from_line and to_line are not provided, read the entire file.")
    suspend fun readFile(
        @LLMDescription("The path to the file to read")
        path: String,
        @LLMDescription("Optional: The starting line number (0-indexed, inclusive)")
        fromLine: Int? = null,
        @LLMDescription("Optional: The ending line number (0-indexed, exclusive)")
        toLine: Int? = null
    ): String {
        suspend fun String.alsoSendFailureEvent(): String =
            also { eventBus.send(AgentEvent.ReadFile.Failure(path, it)) }

        suspend fun String.alsoSendSuccessEvent(): String =
            also { eventBus.send(AgentEvent.ReadFile.Success(path, fromLine, toLine, it)) }

        val file = File(path)
        if (!file.exists())
            return "Error: File not found at path: $path".alsoSendFailureEvent()
        if (!file.isFile)
            return "Error: Path is not a file: $path".alsoSendFailureEvent()
        if (!file.canRead())
            return "Error: Cannot read: $path".alsoSendFailureEvent()
        if (file.length() > 1_000_000)
            return "Error: File is larger than 1MB".alsoSendFailureEvent()
        if (fromLine == null && toLine == null)
            return file.readText().alsoSendSuccessEvent()
        val lines = file.readLines()
        return lines.subList(fromLine ?: 0, toLine ?: lines.lastIndex).joinToString("\n").alsoSendSuccessEvent()
    }

    @Tool
    @LLMDescription("Write or edit a file. If replace_all is true, replace all occurrences of original_content with edited_content. If replace_all is false, only replace if original_content occurs exactly once.")
    suspend fun writeFile(
        @LLMDescription("The path to the file to write")
        path: String,
        @LLMDescription("The original content to replace (can be empty string for new files)")
        originalContent: String,
        @LLMDescription("The new content to write")
        editedContent: String,
        @LLMDescription("If true, replace all occurrences. If false, only replace if original_content occurs exactly once.")
        replaceAll: Boolean
    ): String {
        suspend fun String.alsoSendFailureEvent(): String =
            also { eventBus.send(AgentEvent.WriteFile.Failure(path, it)) }

        suspend fun String.alsoSendSuccessEvent(): String =
            also { eventBus.send(AgentEvent.WriteFile.Success(path, originalContent, editedContent, it)) }

        val file = File(path)
        if (originalContent.isEmpty() && file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        val source = file.readText()
        val occurrences = source.split(originalContent)
        if (occurrences.size < 2)
            return "Error: no occurrences found".alsoSendFailureEvent()
        if (occurrences.size > 2 && !replaceAll)
            return "Error: multiple occurrences found but replaceAll is set to false".alsoSendFailureEvent()
        val result = occurrences.joinToString(separator = editedContent)
        file.writeText(result)
        return "File written successfully".alsoSendSuccessEvent()
    }
}
