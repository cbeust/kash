package com.beust.kash

import com.beust.kash.parser.KashParser
import com.google.inject.Inject
import com.google.inject.Singleton
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.Terminal
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.io.StringReader
import java.nio.file.Paths
import java.util.*

interface LineRunner {
    fun runLine(line: String, inheritIo: Boolean): CommandResult
}

@Suppress("PrivatePropertyName")
@Singleton
class Shell2 @Inject constructor(terminal: Terminal,
        private val engine: Engine, private val context: KashContext,
        builtins: Builtins,
        executableFinder: ExecutableFinder,
        scriptFinder: ScriptFinder,
        builtinFinder: BuiltinFinder) : LineRunner {

    private val log = LoggerFactory.getLogger(Shell2::class.java)

    private val DOT_KASH_JSON = File(System.getProperty("user.home"), ".kash.json")
    private val DOT_KASH_KTS = File(System.getProperty("user.home"), ".kash.kts")
    private val KASH_STRINGS = listOf("Kash.ENV", "Kash.PATHS", "Kash.PROMPT", "Kash.DIRS")

    private val reader: LineReader
    private val directoryStack: Stack<String> get() = engine.directoryStack
    private val commandFinder: CommandFinder
    private val commandRunner: CommandRunner
    private val commandRunner2: CommandRunner2

    init {
        //
        // Read ~/.kash.kts
        //
        if (DOT_KASH_KTS.exists()) {
            try {
                engine.eval(FileReader(DOT_KASH_KTS))
                log.debug("Read $DOT_KASH_KTS")
            } catch(ex: Exception) {
                System.err.println("Errors found while reading $DOT_KASH_KTS: " + ex.message)
            }
        }

        //
        // Configure the line reader with the tab completers
        //
        reader = LineReaderBuilder.builder()
                .completer(StringsCompleter(builtins.commands.keys))
                .completer(StringsCompleter(KASH_STRINGS))
                .completer(FileCompleter(directoryStack))
                .terminal(terminal)
                .build()
        directoryStack.push(File(".").absoluteFile.canonicalPath)
        commandFinder = CommandFinder(listOf(builtinFinder, scriptFinder, executableFinder))
        commandRunner = CommandRunner(builtins, engine, commandFinder, context)
        commandRunner2 = CommandRunner2(builtins, engine, commandFinder, context)

        // Copy the path
        //
        System.getenv("PATH").split(File.pathSeparator).forEach {
            context.paths.add(it)
        }

    }

    fun run() {
        var line = reader.readLine(prompt())
        while (line != null) {
            try {
                val result = runLine(line, inheritIo = true)
                result.display()
            } catch(ex: Exception) {
                System.err.println(ex.message)
            }
            line = reader.readLine(prompt())
        }
    }

    override fun runLine(line: String, inheritIo: Boolean): CommandResult {
//        return newParser(line, inheritIo)
        return oldParser(line, inheritIo)
    }

    private fun runKotlin(line: String): CommandResult {
        log.debug("Detected Kotlin")
        return try {
            val er = engine.eval(line)
            CommandResult(0, er?.toString(), null)
        } catch(ex: Exception) {
            CommandResult(1, null, ex.message)
        }
    }

    private fun newParser(line: String, inheritIo: Boolean): CommandResult {
        val parser = KashParser(StringReader(line))
        var list: KashParser.SimpleList? = null
        var commandSearchResult: CommandFinder.CommandSearchResult? = null
        val result =
            try {
                list = parser.SimpleList()
                var localResult = CommandResult(1)
                val plCommand = list.content[0]
                val command = plCommand.content[0]
                val simpleCommand = command.simpleCommand
                if (simpleCommand != null) {
                    val firstWord = simpleCommand.content[0]
                    commandSearchResult = commandFinder.findCommand(firstWord)
                    if (commandSearchResult == null) {
                        runKotlin(line)
                    } else {
                        commandRunner2.runLine(line, list, commandSearchResult, inheritIo)
                    }
                } else {
                    println("subshell")
                    CommandResult(0)
                }
            } catch(ex: Exception) {
                runKotlin(line)
            }
        return result
    }

    fun oldParser(line: String, inheritIo: Boolean): CommandResult {
        val commands = Parser(::tokenTransformer).parse(line)

        fun logCommand(command: Command, type: String) = log.debug("Type($type), Exec:$command")

        var result: CommandResult? = null
        commands.forEach { command ->
            val firstWord = command.firstWord
            val commandSearchResult = commandFinder.findCommand(firstWord)
            result =
                if (commandSearchResult != null) {
                    logCommand(command, commandSearchResult.type.name)
                    commandRunner.runLine(line, command, commandSearchResult, inheritIo)
                } else {
                    logCommand(command, "Kotlin")
                    try {
                        val er = engine.eval(line)
                        CommandResult(0, er?.toString(), null)
                    } catch(ex: Exception) {
                        CommandResult(1, null, ex.message)
                    }
                }
        }
        return result ?: CommandResult(1, "Couldn't parse $line")
    }

    private fun defaultPrompt(): String {
        val path = Paths.get(directoryStack.peek())
        val size = path.nameCount
        val result = if (size > 2) path.getName(size - 2).toString() + "/" + path.getName(size - 1).toString()
        else path.toString()
        val dollar = "${Ansi.GREEN}$ "
        return result + dollar
    }

    private fun prompt(): String {
        val p = engine.prompt
        return if (p.isBlank()) {
            defaultPrompt()
        } else {
            if (p.startsWith("`") && p.endsWith("`")) {
                val cr = runLine(p.substring(1, p.length - 1), inheritIo = false)
                if (cr.returnCode == 0 && cr.stdout != null) {
                    cr.stdout
                } else if (cr.returnCode != 0) {
                    "Error running command $p: '${cr.stderr!!}  "
                } else {
                    ""
                }
            } else {
                p
            }
        }
    }

    private val tokenTransformers = listOf(
            TildeTransformer(),
            GlobTransformer(directoryStack),
            BackTickTransformer(this),
            EnvVariableTransformer(context.env)
    )

    private fun tokenTransformer(token: Token.Word, words: List<String>): List<String> {
        val result = ArrayList(words)
        log.trace("    Transforming $words")
        tokenTransformers.forEach { t ->
            val transformed = ArrayList<String>(t.transform(token, result))
            result.clear()
            result.addAll(transformed)
            log.trace("    After " + t.javaClass + ": " + transformed)
        }
        return result
    }

}