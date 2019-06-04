package com.beust.kash

import java.io.File
import java.io.FileReader
import java.util.*
import javax.script.ScriptEngine
import kotlin.reflect.KFunction1

interface BuiltinContext {
    val env: Map<String, String>
    val paths: List<String>
    val directoryStack: Stack<String>

    fun findCommand(word: String) : Result<String>
}

class Builtins(private val context: BuiltinContext, val engine: ScriptEngine) {
    val commands: HashMap<String, KFunction1<List<String>, Shell.CommandResult>> = hashMapOf(
            "cd" to ::cd,
            "pwd" to ::pwd,
            "." to ::dot,
            "env" to ::env,
            "which" to ::which
    )

    private fun env(words: List<String>): Shell.CommandResult {
        val r = engine.eval("Kash.ENV") as Map<String, String>
        return Shell.CommandResult(0, r.toString(), null)
    }

    private fun which(words: List<String>): Shell.CommandResult {
        val commandResult = context.findCommand(words[1])
        val command = commandResult.result

        if (command != null) {
            return Shell.CommandResult(0, command)
        } else {
            return Shell.CommandResult(1, commandResult.errorMessage)
        }
    }

    private fun dot(words: List<String>): Shell.CommandResult {
        val r = engine.eval(FileReader(File(words[1])))
        println("Running script")

        return Shell.CommandResult(0)
    }

    private fun cd(words: List<String>): Shell.CommandResult {
        val stack = context.directoryStack
        val currentDir = stack.peek()
        val dir = File(currentDir, words[1])
        val rc =
            if (words[1] == "-" && ! stack.isEmpty()) {
                stack.pop()
                val cd = if (stack.size == 1) stack.peek() else stack.pop()
                val newDir = File(".").absoluteFile.canonicalPath
                stack.push(newDir)
                println(cd)
                0
            } else if (dir.exists()) {
                val newDir = dir.absoluteFile.canonicalPath
                stack.push(newDir)
                println(newDir)
                0
            } else {
                System.err.println("Directory not found: $dir")
                1
            }
        return Shell.CommandResult(rc)
    }

    private fun pwd(words: List<String>): Shell.CommandResult {
        val stdout =
            if (context.directoryStack.isEmpty()) {
                File(".").toString()
            } else {
                context.directoryStack.peek()
            }
        return Shell.CommandResult(0, stdout, null)
    }
}