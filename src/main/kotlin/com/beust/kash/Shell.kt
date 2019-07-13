package com.beust.kash

import com.beust.kash.parser.KashParser
import com.beust.kash.parser.SimpleCommand
import com.beust.kash.parser.SimpleList
import com.beust.kash.parser.TokenMgrError
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

@Suppress("PrivatePropertyName")
@Singleton
class Shell @Inject constructor(
        private val terminal: Terminal,
        private val engine: Engine,
        private val context: KashContext,
        private val builtins: Builtins,
        private val executableFinder: ExecutableFinder,
        private val scriptFinder: ScriptFinder,
        private val builtinFinder: BuiltinFinder) : LineRunner {

    private val log = LoggerFactory.getLogger(Shell::class.java)

    private val DOT_KASH_KTS = File(System.getProperty("user.home"), ".kash.kts")
    private val KASH_STRINGS = listOf("Kash.ENV", "Kash.PATHS", "Kash.PROMPT", "Kash.DIRS")

    private val reader: LineReader
    private val directoryStack: Stack<String> get() = engine.directoryStack
    private val commandFinder: CommandFinder
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
        return newParser(line, inheritIo)
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

    private fun transform(list: SimpleList) {
        list.content.forEach { pipeLineCommand ->
            pipeLineCommand.content.forEach { command ->
                if (command.simpleCommand != null) {
                    tokenTransformer2(command.simpleCommand)
                } else if (command.subShell != null) {

                } else {
                    throw IllegalArgumentException("Unexpected command content: $command")
                }
            }
        }
    }

    private fun newParser(line: String, inheritIo: Boolean): CommandResult {
        val parser = KashParser(StringReader(line))
        val list: SimpleList?
        val commandSearchResult: CommandFinder.CommandSearchResult?
        val result =
            try {
                list = parser.SimpleList()
                if (list.content.isNotEmpty()) {
                    transform(list)
                    val plCommand = list.content[0]
                    val command = plCommand.content[0]
                    val simpleCommand = command.simpleCommand
                    if (simpleCommand != null) {
                        val firstWord = simpleCommand.words[0]
                        commandSearchResult = commandFinder.findCommand(firstWord)
                        if (commandSearchResult == null) {
                            runKotlin(line)
                        } else {
                            commandRunner2.runLine(line, list, commandSearchResult, inheritIo)
                        }
                    } else {
                        val shell = Shell(terminal, engine, context, builtins, executableFinder, scriptFinder,
                                builtinFinder)
                        val newLine = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"))
                        if (list.ampersand) {
                            Background.launchBackgroundCommand {
                                shell.runLine(newLine, inheritIo)
                            }
                        } else {
                            shell.runLine(newLine, inheritIo)
                        }
                    }
                } else {
                    CommandResult(0)
                }
            } catch(ex: TokenMgrError) {
                println("Exception: ${ex.message}")
                runKotlin(line)
            }
        return result
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

    private val tokenTransformers: List<TokenTransformer> = listOf(
            TildeTransformer(),
            GlobTransformer(directoryStack),
            BackTickTransformer(this),
            EnvVariableTransformer(context.env)
    )

    private fun tokenTransformer2(command: SimpleCommand) {
        val words = command.content
        val result = command.content
        log.trace("    Transforming $words")
        tokenTransformers.forEach { t ->
            val transformed = t.transform(command, result)
            log.trace("    After ${t::class}: $transformed")
            result.clear()
            result.addAll(transformed)
        }
        command.words = result.flatMap { it.content }
    }
}