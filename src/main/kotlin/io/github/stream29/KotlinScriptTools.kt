package io.github.stream29

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.mainKts.MainKtsScript
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

@Suppress("unused")
@LLMDescription("Tool to run Kotlin scripts")
class KotlinScriptTools : ToolSet {
    @Serializable
    enum class ScriptOutputMode {
        Console, Return
    }

    private val host = BasicJvmScriptingHost()
    private fun evalUnsafe(script: String): ResultWithDiagnostics<EvaluationResult> =
        host.evalWithTemplate<MainKtsScript>(
            script = script.toScriptSource(),
            evaluation = {
                constructorArgs(emptyArray<String>())
            }
        )

    private fun eval(script: String): ScriptEvaluationResult =
        try {
            val (consoleOutput, resultWithDiagnostics) = captureOutput { evalUnsafe(script) }
            when (resultWithDiagnostics) {
                is ResultWithDiagnostics.Success<*> ->
                    ScriptEvaluationResult.Success(resultWithDiagnostics, consoleOutput)

                is ResultWithDiagnostics.Failure ->
                    ScriptEvaluationResult.CompilationFailure(
                        resultWithDiagnostics.reports.joinToString("\n") { it.message }
                    )
            }
        } catch (e: Exception) {
            ScriptEvaluationResult.RuntimeFailure(e)
        }

    private inline fun <T> captureOutput(block: () -> T): Pair<String, T> {
        @OptIn(ExperimentalContracts::class)
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val originalOut = System.out
        val originalErr = System.err
        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream)
        System.setOut(printStream)
        System.setErr(printStream)
        val result = try {
            block()
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
        val output = outputStream.toString()
        return output to result
    }

    sealed interface ScriptEvaluationResult {
        data class CompilationFailure(val message: String) : ScriptEvaluationResult
        data class RuntimeFailure(val exception: Exception) : ScriptEvaluationResult
        data class Success(val returnValue: Any?, val consoleOutput: String) : ScriptEvaluationResult
    }

    @Tool
    @LLMDescription("Run a Kotlin MainKts script. The script can use Maven dependencies with @file:DependsOn annotation. The last line of the script is the return value.")
    fun runKotlin(
        @LLMDescription("The Kotlin MainKts script to execute")
        script: String,
        @LLMDescription("Output mode: 'Console' to capture console output, 'Return' to get the return value")
        output: ScriptOutputMode
    ): String {
        return when (val result = eval(script)) {
            is ScriptEvaluationResult.Success -> when (output) {
                ScriptOutputMode.Console -> result.consoleOutput
                ScriptOutputMode.Return -> result.returnValue.toString()
            }

            is ScriptEvaluationResult.CompilationFailure -> "Compilation failed: \n${result.message}"
            is ScriptEvaluationResult.RuntimeFailure -> "Execution failed: \n${result.exception.stackTraceToString()}"
        }
    }
}