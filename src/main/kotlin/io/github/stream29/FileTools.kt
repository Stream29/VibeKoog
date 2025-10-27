package io.github.stream29

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import org.jetbrains.kotlin.mainKts.MainKtsScript
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.api.onFailure
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

@Suppress("unused")
@LLMDescription("Tools to write or read files.")
class FileTools : ToolSet {
    @Tool
    @LLMDescription("Read the content of a file. If from_line and to_line are not provided, read the entire file.")
    fun readFile(
        @LLMDescription("The path to the file to read")
        path: String,
        @LLMDescription("Optional: The starting line number (0-indexed, inclusive)")
        fromLine: Int? = null,
        @LLMDescription("Optional: The ending line number (0-indexed, exclusive)")
        toLine: Int? = null
    ): String {
        val file = File(path)
        if (!file.exists())
            return "Error: File not found at path: $path"
        if (!file.isFile)
            return "Error: Path is not a file: $path"
        if (!file.canRead())
            return "Error: Cannot read: $path"
        if (file.length() > 1_000_000)
            return "Error: File is larger than 1MB"
        if (fromLine == null && toLine == null)
            return file.readText()
        val lines = file.readLines()
        return lines.subList(fromLine ?: 0, toLine ?: lines.lastIndex).joinToString("\n")
    }

    @Tool
    @LLMDescription("Write or edit a file. If replace_all is true, replace all occurrences of original_content with edited_content. If replace_all is false, only replace if original_content occurs exactly once.")
    fun writeFile(
        @LLMDescription("The path to the file to write")
        path: String,
        @LLMDescription("The original content to replace (can be empty string for new files)")
        originalContent: String,
        @LLMDescription("The new content to write")
        editedContent: String,
        @LLMDescription("If true, replace all occurrences. If false, only replace if original_content occurs exactly once.")
        replaceAll: Boolean
    ): String {
        val file = File(path)
        if (originalContent.isEmpty() && file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        val source = file.readText()
        val occurrences = source.split(originalContent)
        if (occurrences.size < 2)
            return "Error: no occurrences found"
        if (occurrences.size > 2 && !replaceAll)
            return "Error: multiple occurrences found but replaceAll is set to false"
        return occurrences.joinToString(separator = editedContent)
    }
}
