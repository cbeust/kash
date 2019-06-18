package com.beust.kash

import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.Terminal
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.nio.file.Paths
import java.util.*
import javax.script.ScriptEngineManager

interface LineRunner {
    fun runLine(line: String, inheritIo: Boolean): CommandResult
}

class Shell2(terminal: Terminal) : LineRunner {
    private val log = LoggerFactory.getLogger(Shell2::class.java)

    private val DOT_KASH = File(System.getProperty("user.home"), ".kash.json")
    private val PREDEF = "kts/Predef.kts"
    private val KASH_STRINGS = listOf("Kash.ENV", "Kash.PATHS", "Kash.PROMPT", "Kash.DIRS")

    private val reader: LineReader
    private val builtins: Builtins
    private val engine: Engine
    private val directoryStack: Stack<String> get() = engine.directoryStack
    private val commandFinder: CommandFinder
    private val commandRunner: CommandRunner
    private val context: KashContext
    private val executableFinder: ICommandFinder
    private val builtinsFinder: ICommandFinder
    private val scriptFinder: ICommandFinder

    init {
        val kotlinEngine = ScriptEngineManager().getEngineByExtension("kash.kts")
                ?: throw IllegalArgumentException("Couldn't find a script engine for .kash.kts")
        //
        // Read Predef
        //
        val predef = InputStreamReader(this::class.java.classLoader.getResource(PREDEF).openStream())
        kotlinEngine.eval(predef)
        log.debug("Read $PREDEF")

        engine = Engine(kotlinEngine)
        context = KashContext(engine)

        //
        // Configure the line reader with the tab completers
        //

        executableFinder = ExecutableFinder(context.paths)
        builtins = Builtins(context, engine, executableFinder)
        reader = LineReaderBuilder.builder()
                .completer(StringsCompleter(builtins.commands.keys))
                .completer(StringsCompleter(KASH_STRINGS))
                .completer(FileCompleter(directoryStack))
                .terminal(terminal)
                .build()
        directoryStack.push(File(".").absoluteFile.canonicalPath)
        builtinsFinder = BuiltinFinder(builtins)
        scriptFinder = ScriptFinder(context.scriptPath)
        commandFinder = CommandFinder(listOf(builtinsFinder, scriptFinder, executableFinder))
        commandRunner = CommandRunner(builtins, engine, commandFinder, context)

        //
        // Read ~/.kash.kts
        //
        if (DOT_KASH.exists()) {
            try {
                engine.eval(FileReader(DOT_KASH))
                log.debug("Read $DOT_KASH")
            } catch(ex: Exception) {
                System.err.println("Errors found while reading $DOT_KASH: " + ex.message)
            }
        }

        //
        // Read ~/.kash.json
        //
        DotKashReader.dotKash?.scriptPath?.let {
            context.scriptPath.addAll(it)
        }

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
                if (result.stdout != null) {
                    println(result.stdout)
                }
                if (result.stderr != null) {
                    println(result.stderr)
                }
            } catch(ex: Exception) {
                System.err.println(ex.message)
            }
            line = reader.readLine(prompt())
        }
    }

    override fun runLine(line: String, inheritIo: Boolean): CommandResult {
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

    private fun prompt(): String {
        val path = Paths.get(directoryStack.peek())
        val size = path.nameCount
        val result = if (size > 2) path.getName(size - 2).toString() + "/" + path.getName(size - 1).toString()
        else path.toString()
        val dollar = "${Ansi.GREEN}$ "
        return result + dollar
    }

    private val tokenTransformers = listOf<TokenTransformer>(
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