package com.beust.kash

import com.beust.kash.api.CommandResult
import com.beust.kash.api.IKashContext
import com.beust.kash.parser.KashParser
import com.beust.kash.parser.SimpleCommand
import com.beust.kash.parser.SimpleList
import com.beust.kash.parser.TokenMgrError
import com.google.inject.Inject
import com.google.inject.Singleton
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.completer.AggregateCompleter
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.Terminal
import org.slf4j.LoggerFactory
import java.io.File
import java.io.StringReader
import java.nio.file.Paths
import java.util.*

@Suppress("PrivatePropertyName")
@Singleton
class Shell @Inject constructor(
        private val terminal: Terminal,
        private val engine: Engine,
        private val kashObject: KashObject,
        private val builtins: Builtins,
        private val executableFinder: ExecutableFinder,
        private val scriptFinder: ScriptFinder) : LineRunner {

    private val log = LoggerFactory.getLogger(Shell::class.java)

    private val KASH_STRINGS = listOf("Kash.ENV", "Kash.PATHS", "Kash.PROMPT", "Kash.DIRS")

    private val builtinFinder: BuiltinFinder
    private val reader: LineReader
    private val directoryStack: Stack<String> get() = kashObject.directoryStack
    private val commandFinder: CommandFinder

    init {
        val context = KashContext(engine)

        val completers = arrayListOf<Completer>()
        val extensions = arrayListOf<String>()

        val dotKash = DotKashJsonReader.dotKash
        if (dotKash != null) {
            if (dotKash.completers.isNotEmpty()) {
                completers.add(ExternalCompleter(context, engine))
            }
            extensions.addAll(dotKash.extensions)
        }

        builtinFinder = BuiltinFinder(listOf(builtins), extensions)

        completers.addAll(listOf(
                StringsCompleter(builtinFinder.builtinMap.keys),
                StringsCompleter(KASH_STRINGS),
                FileCompleter(directoryStack))
        )

        //
        // Configure the line reader with the tab completers
        //

        val builder = LineReaderBuilder.builder()
                .completer(AggregateCompleter(completers))
                .terminal(terminal)
        reader = builder.build()
        directoryStack.push(File(".").absoluteFile.canonicalPath)
        commandFinder = CommandFinder(listOf(builtinFinder, scriptFinder, executableFinder))
    }

    fun run() {
        val context = KashContext(engine)
        var line = reader.readLine(prompt(context))
        while (line != null) {
            try {
                val result = runLine(line, context, inheritIo = true)
                result.display()
            } catch(ex: Exception) {
                System.err.println(ex.message)
            }
            line = reader.readLine(prompt(context))
        }
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

    override fun runLine(line: String, context: IKashContext, inheritIo: Boolean): CommandResult {
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
                    //
                    // Either it's a simple command or a subshell (surrounded by parentheses)
                    //
                    val simpleCommand = command.simpleCommand
                    if (simpleCommand != null) {
                        // Simple command
                        val firstWord = simpleCommand.words[0]
                        commandSearchResult = commandFinder.findCommand(firstWord, list, context)
                        if (commandSearchResult == null) {
                            runKotlin(line)
                        } else {
                            commandSearchResult.lambda()
                        }
                    } else {
                        // Subshell, create a new Shell and run that command in it
                        val shell = Shell(terminal, engine, kashObject, builtins, executableFinder, scriptFinder)
                        val newLine = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"))
                        if (list.ampersand) {
                            Background.launchBackgroundCommand {
                                shell.runLine(newLine, context, inheritIo)
                            }
                        } else {
                            shell.runLine(newLine, context, inheritIo)
                        }
                    }
                } else {
                    CommandResult(0)
                }
            } catch(ex: TokenMgrError) {
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

    private fun prompt(context: IKashContext): String {
        val p = kashObject.prompt
        return if (p.isBlank()) {
            defaultPrompt()
        } else {
            if (p.startsWith("`") && p.endsWith("`")) {
                val cr = runLine(p.substring(1, p.length - 1), context, inheritIo = false)
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
            BackTickTransformer(this, KashContext(engine)),
            EnvVariableTransformer(KashContext(engine).env)
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