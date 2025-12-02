package io.github.stream29.vibekoog

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.File

class ToolsTest {

    @Test
    fun testWriteAndReadFile() = runBlocking {
        val file = File("test_file.txt")
        val writeFile = WriteFile()
        val readFile = ReadFile()

        try {
            // Test Write
            val writeArgs = WriteFile.Args(
                path = file.absolutePath,
                edited_content = "Hello\nWorld\nKotlin",
                replace_all = true
            )
            writeFile.execute(writeArgs)
            
            assertTrue(file.exists())
            assertEquals("Hello\nWorld\nKotlin", file.readText())

            // Test Read with lines
            val readArgs = ReadFile.Args(
                path = file.absolutePath,
                from_line = 2,
                to_line = 2
            )
            val result = readFile.execute(readArgs)
            assertEquals("World", result)
        } finally {
            file.delete()
        }
    }

    @Test
    fun testRunKotlin() = runBlocking {
        val runKotlin = RunKotlin()
        val script = """
            val x = 10
            val y = 20
            println("Sum: " + (x + y))
            x + y
        """.trimIndent()

        // Test Console Output
        val consoleArgs = RunKotlin.Args(script = script, output = "console")
        val consoleResult = runKotlin.execute(consoleArgs)
        // Use trim() to handle potential newlines
        assertTrue(consoleResult.trim().contains("Sum: 30"))

        // Test Return Value
        val returnArgs = RunKotlin.Args(script = script, output = "return")
        val returnResult = runKotlin.execute(returnArgs)
        assertEquals("30", returnResult)
    }
}
