package com.beust.kash

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.google.inject.Inject
import java.io.File
import java.io.FileReader
import java.util.*
import kotlin.reflect.KFunction1



class Builtins @Inject constructor(private val context: KashContext, val engine: Engine,
        private val executableFinder: ExecutableFinder) {
    val commands: HashMap<String, KFunction1<List<String>, CommandResult>> = hashMapOf(
            "cd" to ::cd,
            "pwd" to ::pwd,
            "." to ::dot,
            "env" to ::env,
            "which" to ::which,
            "log" to ::log
    )

    private fun log(words: List<String>): CommandResult {
        val root = org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        if (root.level == Level.DEBUG) root.level = Level.OFF
            else root.level = Level.DEBUG
        return CommandResult(0)
    }

    private fun env(words: List<String>): CommandResult {
        val r = engine.env
        return CommandResult(0, r.toString(), null)
    }

    private fun which(words: List<String>): CommandResult {
        val commandResult = executableFinder.findCommand(words[1])
        val command = commandResult?.path

        if (command != null) {
            return CommandResult(0, command)
        } else {
            return CommandResult(1)
        }
    }

    private fun dot(words: List<String>): CommandResult {
        println("Running script " + words[1])
        val r = engine.eval(FileReader(File(words[1])), words.subList(2, words.size))

        return CommandResult(0)
    }

    private fun cd(words: List<String>): CommandResult {
        val stack = context.directoryStack
        val currentDir = stack.peek()
        val targetDir = File(words[1])
        val dir =
                if (targetDir.isAbsolute) targetDir
                else File(currentDir, words[1])
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
        return CommandResult(rc)
    }

    private fun pwd(words: List<String>): CommandResult {
        val stdout =
            if (context.directoryStack.isEmpty()) {
                File(".").toString()
            } else {
                context.directoryStack.peek()
            }
        return CommandResult(0, stdout, null)
    }
}