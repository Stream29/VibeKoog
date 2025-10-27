package io.github.stream29

import kotlinx.coroutines.CompletableDeferred

sealed interface AgentEvent {
    suspend fun handle()
    sealed interface ReadFile : AgentEvent {
        data class Success(
            val path: String,
            val fromLine: Int?,
            val toLine: Int?,
            val content: String
        ) : ReadFile {
            override suspend fun handle() {
                if (fromLine != null && toLine != null)
                    println("Read file: $path:${fromLine}-${toLine} (${content.length} chars)")
                else
                    println("Read file: $path (${content.length} chars)")
            }
        }

        data class Failure(
            val path: String,
            val error: String
        ) : ReadFile {
            override suspend fun handle() {
                println("Error reading file: $path: $error")
            }
        }
    }

    sealed interface WriteFile : AgentEvent {
        data class Success(
            val path: String,
            val originalContent: String,
            val editedContent: String,
            val result: String
        ) : WriteFile {
            override suspend fun handle() {
                println("File written: $path")
            }
        }

        data class Failure(
            val path: String,
            val error: String
        ) : WriteFile {
            override suspend fun handle() {
                println("Error writing file: $path: $error")
            }
        }
    }

    sealed interface RunKotlin : AgentEvent {
        data class Success(
            val script: String,
            val output: String
        ) : RunKotlin {
            override suspend fun handle() {
                println("Script executed: $script")
            }
        }

        data class CompilationFailure(
            val script: String,
            val error: String
        ) : RunKotlin {
            override suspend fun handle() {
                println("Compilation error in script: $script: $error")
            }
        }

        data class RuntimeFailure(
            val script: String,
            val error: String
        ) : RunKotlin {
            override suspend fun handle() {
                println("Runtime error in script: $script: $error")
            }
        }
    }

    data class WaitForUserInput(val deferred: CompletableDeferred<String>) : AgentEvent {
        override suspend fun handle() {
            print("[User]: ")
            deferred.complete(readln())
        }
    }

    data class SayToUser(val message: String) : AgentEvent {
        override suspend fun handle() {
            println("[Koog Agent]: $message")
        }
    }
}