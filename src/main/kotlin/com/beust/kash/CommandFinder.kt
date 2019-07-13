package com.beust.kash

import com.google.inject.Inject
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader

enum class CommandType { COMMAND, SCRIPT, BUILT_IN, OTHER }

interface ICommandFinder {
    fun findCommand(word: String): CommandFinder.CommandSearchResult?
}

class ExecutableFinder @Inject constructor(private val context: KashContext): ICommandFinder {
    /**
     * Need to introduce two implementations of this method: one for Windows and one for others.
     * For Windows, need to 1) look up commands that end with .exe, .cmd, .bat and 2) manually
     * add support for #! scripts.
     */
    override fun findCommand(word: String): CommandFinder.CommandSearchResult? {
        val paths = context.paths
        // See if we can find this command on the path
        paths.forEach { path ->
            listOf("", ".exe", ".cmd").forEach { suffix ->
                val c1 =
                        if (word.startsWith(".") || word.startsWith("/")) word
                        else path + File.separatorChar + word + suffix
                val file = File(c1)
                if (file.exists() and file.isFile and file.canExecute()) {
                    val chars = CharArray(2)
                    FileReader(File(c1)).read(chars, 0, 2)
                    if (chars[0] == '#' && chars[1] == '!') {
                        // TOOD on Windows: shebang script
                    }
                    return CommandFinder.CommandSearchResult(CommandType.COMMAND, File(c1).absolutePath)
                }
            }
        }
        return null
    }
}

class ScriptFinder @Inject constructor (private val context: KashContext): ICommandFinder {
    private val log = LoggerFactory.getLogger(ScriptFinder::class.java)

    override fun findCommand(word: String): CommandFinder.CommandSearchResult? {
        val scriptPath = context.scriptPaths
        // See if this is a .kash.kts script
        scriptPath.forEach { path ->
            val c1 =
                    if (word.startsWith(".") || word.startsWith("/")) word
                    else path + File.separatorChar + word
            val file = File("$c1.kash.kts")
            if (file.exists() and file.isFile) {
                log.debug("Found script: " + file.absolutePath)
                return CommandFinder.CommandSearchResult(CommandType.SCRIPT, file.absolutePath)
            }
        }

        return null
    }
}

class BuiltinFinder @Inject constructor (private val builtins: Builtins): ICommandFinder {
    override fun findCommand(word: String) =
        if (builtins.commands[word] != null) {
            CommandFinder.CommandSearchResult(CommandType.BUILT_IN, word)
        } else {
            null
        }
}

class CommandFinder(private val finders: List<ICommandFinder>): ICommandFinder {
    /**
     * Describe the result of search for a command. A command can be an executable, a script, or a built-in command.
     */
    class CommandSearchResult(val type: CommandType, val path: String)

    override fun findCommand(word: String): CommandSearchResult? {
        return finders.mapNotNull { it.findCommand(word) }.firstOrNull()
    }
}