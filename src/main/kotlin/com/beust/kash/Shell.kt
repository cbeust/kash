package com.beust.kash

import com.beust.kash.Streams.readStream
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.Terminal
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager


interface CommandRunner {
    fun runLine(line: String, inheritIo: Boolean): Shell.CommandResult
}

@Suppress("PrivatePropertyName", "UNCHECKED_CAST")
class Shell(terminal: Terminal): BuiltinContext, CommandRunner {
    private val log = LoggerFactory.getLogger(Shell::class.java)

    override val directoryStack: Stack<String> get() = engine.eval("Kash.DIRS") as Stack<String>
    override val env: HashMap<String, String> get() = engine.eval("Kash.ENV") as HashMap<String, String>
    override val paths: ArrayList<String> get() = engine.eval("Kash.PATHS") as ArrayList<String>
    private var prompt: String
        get() = engine.eval("Kash.PROMPT") as String
        set(s) { engine.eval("Kash.PROMPT = $s") }
    private val reader: LineReader
    private val builtins: Builtins
    private val engine: ScriptEngine = ScriptEngineManager().getEngineByExtension("kts")

    private val DOT_KASH = File(System.getProperty("user.home"), ".kash.kts")
    private val PREDEF = "kts/Predef.kts"
    private val KASH_STRINGS = listOf("Kash.ENV", "Kash.PATHS", "Kash.PROMPT", "Kash.DIRS")

//    class MyScriptEngineFactory(private val classpath: List<File>) : KotlinJsr223JvmScriptEngineFactoryBase() {
//        override fun getScriptEngine(): ScriptEngine =
//                KotlinJsr223JvmLocalScriptEngine(
//                        this,
//                        classpath, // !!! supply the script classpath here
//                        KotlinStandardJsr223ScriptTemplate::class.qualifiedName!!,
//                        { ctx, types -> ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), types ?: emptyArray()) },
//                        arrayOf(Bindings::class)
//                )
//    }

    init {
        //
        // Read Predef
        //
        val predef = InputStreamReader(this::class.java.classLoader.getResource(PREDEF).openStream())
        engine.eval(predef)
        log.debug("Read $PREDEF")

        //
        // Read ~/.kash.json, configure the classpath of the script engine
        //
//        val dotKashReader = DotKashReader()
//        val su = kotlinJar("kotlin-script-util")
//        val classpath = dotKashReader.dotKash?.classpath?.map { File(it) } ?: emptyList()
//        val jars = listOf<File>()//KotlinJars.compilerClasspath + classpath

//        engine = MyScriptEngineFactory(jars).scriptEngine
        builtins = Builtins(this, engine)
        reader = LineReaderBuilder.builder()
                .completer(StringsCompleter(builtins.commands.keys))
                .completer(StringsCompleter(KASH_STRINGS))
                .completer(FileCompleter(directoryStack))
                .terminal(terminal)
                .build()

        directoryStack.push(File(".").absoluteFile.canonicalPath)

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
        // Copy the path
        //
        System.getenv("PATH").split(File.pathSeparator).forEach {
            paths.add(it)
        }
    }

//    private fun extractDirAndVersion(path: String): Pair<String, String> {
//        val org = "/org.jetbrains.kotlin"
//        val index = path.lastIndexOf(org)
//        val baseDir = path.substring(0, index + org.length + 1)
//        val segments = path.substring(baseDir.length).split(File.separator)
//
//        return Pair(baseDir, segments[1])
//    }

//    private fun kotlinJar(libName: String): String {
//        val cp = System.getProperty("java.class.path").split(File.pathSeparator).filter {
//            it.contains("kotlin")
//        }
//        val su = cp.first { it.contains("/org.jetbrains.kotlin") }
//        val (baseDir, version) = extractDirAndVersion(su)
//        val result = "$baseDir$libName/$version/$libName-$version.jar"
//        return result
//    }

    private val tokenTransformers = listOf(
            TildeTransformer(), GlobTransformer(directoryStack), BackTickTransformer(this), EnvVariableTransformer(env)
    )

    private fun tokenTransformer(token: Token.Word, words: List<String>): List<String> {
        val result = ArrayList(words)
        tokenTransformers.forEach { t ->
            val transformed = t.transform(token, result)
            result.clear()
            result.addAll(transformed)
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
        val p = prompt
        return if (p.isBlank()) {
            defaultPrompt()
        } else {
            if (p.startsWith("`") && p.endsWith("`")) {
                val cr = runLine(p.substring(1, p.length - 1), inheritIo = false)
                if (cr.returnCode == 0 && cr.stdout != null) {
                    cr.stdout
                } else {
                    "Error running command $p: '${cr.stderr!!}  "
                }
            } else {
                p
            }
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
        var result = CommandResult(0)
        val commands = Parser(::tokenTransformer).parse(line)

        commands.forEach { command ->
            val firstWord = command.firstWord
            val pathCommand = findCommand(firstWord).result
            val builtinCommand = builtins.commands[firstWord]
            fun logCommand(type: String)
                    = log.debug("Type($type), Exec:$command")
            result =
                if (builtinCommand != null) {
                    // Built-in command
                    logCommand("Built-in")
                    builtinCommand(command.words)
                } else if (pathCommand != null) {
                    // Shell command
                    logCommand("Command")
                    runCommand(command, inheritIo)
                } else {
                    // Kotlin
                    logCommand("Kotlin")
                    try {
                        val res = engine.eval(line)
                        CommandResult(0, res?.toString(), null)
                    } catch(ex: Exception) {
                        CommandResult(1, null, ex.message)
                    }
                }

        }
        return result
    }

    private fun findPath(words: List<Token>): List<String>? {
        val commandResult = findCommand((words[0] as Token.Word).name[0])
        val pathCommand = commandResult.result
        val result = arrayListOf<String>().apply { add(pathCommand!!) }
        var i = 1
        while (i < words.size) {
            val w = words[i]
            when (w) {
                is Token.Greater, is Token.Less -> {
                    i++
                }
                is Token.Word -> {
                    result.addAll(w.name)
                }
            }
            i++
        }
        val result2 = result.map { it.replace('/', File.separatorChar) }
        val command = File(result2[0])
        return result
    }

    private fun execToProcessBuilder(exec: Exec, inheritIo: Boolean): ProcessBuilder {
        ProcessBuilder(findPath(exec.tokens)).let { result ->
            var redirect = false
            exec.input?.let {
                redirect = true
                result.redirectInput(File(it))
            }
            exec.output?.let {
                redirect = true
                result.redirectOutput(File(it))
            }
            if (! directoryStack.isEmpty()) {
                result.directory(File(directoryStack.peek()))
            }
            // Copy the native shell environment into Kash before launching the process
            val pbEnv = result.environment()
            env.entries.forEach { entry ->
                pbEnv[entry.key] = entry.value
            }
            if (inheritIo && ! redirect) result.inheritIO()
            return result
        }
    }

    private fun runCommand(command: Command, inheritIo: Boolean): CommandResult {
        val result: CommandResult = when (command) {
            is Command.SingleCommand -> {
                // A single command
                launchCommand(command.exec, inheritIo)
            }
            is Command.PipeCommands -> {
                // Commands piped between each other
                val builders = command.execs.map { exec ->
                    execToProcessBuilder(exec, inheritIo = false)
                }
                val r = ProcessBuilder.startPipeline(builders)
                r[r.size - 1].let { lastProcess ->
                    val output = readStream(lastProcess.inputStream)
                    val error = readStream(lastProcess.errorStream)
                    CommandResult(0, output, error)
                }
            }
            is Command.AndCommands -> {
                // Commands separated by &&: only runLine n+1 if n returned 0
                log.debug("Command: $command")
                var i = 0
                var lastReturnCode = 0
                var result: CommandResult? = null
                while (i < command.execs.size && lastReturnCode == 0) {
                    val exec = command.execs[i]
                    log.debug("Launching $exec")
                    val path = findCommand(exec.words[0]).result
                    if (path != null) {
                        result = launchCommand(exec, inheritIo)
                        if (i < command.execs.size - 1) println(result.stdout)
                        lastReturnCode = result.returnCode
                        i++
                    } else {
                        throw java.lang.IllegalArgumentException("Couldn't find " + exec.words[0])
                    }
                }
                result!!
            }
        }
        return result
    }

    class CommandResult(val returnCode: Int, val stdout: String? = null, val stderr: String? = null)

    private fun launchCommand(exec: Exec, inheritIo: Boolean): CommandResult {
        //
        // Extract all the A=B statements from that command and store them in commandEnv
        //
        val commandEnv = hashMapOf<String, String>()
        var i = 0
        val newTokens = arrayListOf<Token>()
        while (i < exec.tokens.size) {
            val w = exec.tokens[i]
            if (w is Token.Word) {
                var addToken = false
                w.name.forEach { n ->
                    if (n.contains("=")) {
                        val split = n.split("=")
                        commandEnv[split[0]] = split[1]
                    } else {
                        addToken = true
                    }
                }
                if (addToken) newTokens.add(w)

            }
            else {
                newTokens.add(w)
            }
            i++
        }

        if (newTokens.isEmpty()) {
            //
            // Just defining new environment variables (e.g. "A=B"), add them to the global environment
            //
            commandEnv.entries.forEach {
                env[it.key] = it.value
                log.debug("Defined ${it.key}=${it.value}")
            }
            return CommandResult(0)
        } else {
            //
            // Defining environment variables and launching a new process, e.g. "A=B ls"
            // Add this environment only to this process launch, not to the global environment
            //

            val newExec = Exec(newTokens, exec.input, exec.output, exec.transform)
            val pb = execToProcessBuilder(newExec, inheritIo)
            commandEnv.entries.forEach {
                pb.environment()[it.key] = it.value
                log.debug("Defined locally ${it.key}=${it.value}")
            }

            log.debug("Launching " + pb.command().joinToString(" "))

            if (newTokens.contains(Token.And())) {
                val future = backgroundProcessesExecutor.submit<Void> {
                    val process = pb.start()
                    onFinish(BackgroundCommandResult(42, process.exitValue()))
                    null
                }
                return CommandResult(0)
            } else {
                val process = pb.start()
                val returnCode = process.waitFor()
                val stdout = readStream(process.inputStream)
                val stderr = readStream(process.errorStream)
                return CommandResult(returnCode, stdout, stderr)
            }
        }
    }

    fun onFinish(result: BackgroundCommandResult) {
        println("Background command completed: $result")
    }

    class BackgroundCommand(val id: Int, val command: String)
    data class BackgroundCommandResult(val id: Int, val result: Int)
    private val backgroundProcessesExecutor: ExecutorService = Executors.newFixedThreadPool(10)
    private val backgroundProcesses = hashMapOf<Int, BackgroundCommand>()

    override fun findCommand(word: String) : Result<String> {
        // See if we can find this command on the path
        paths.forEach { path ->
            listOf("", ".exe", ".cmd").forEach { suffix ->
                val c1 =
                    if (word.startsWith(".") || word.startsWith("/")) word
                    else path + File.separatorChar + word + suffix
                val file = File(c1)
                if (file.exists() and file.isFile and file.canExecute()) {
                    return Result(File(c1).absolutePath)
                }
            }
        }
        // See if this word is an environment assignment
//        if (word.contains("=")) {
//            return Result(word)
//        }

        return Result(null, "No \"$word\" found in " + paths.joinToString(", "))
    }
}

object Streams {
    fun readStream(ins: InputStream): String? {
        val reader = BufferedReader(InputStreamReader(ins))

        val builder = StringBuilder()
        var line = reader.readLine()
        while (line != null) {
            builder.append(line)
            builder.append(System.getProperty("line.separator"))
            line = reader.readLine()
        }
        val result = builder.toString()
        return if (result.isBlank()) null else result
    }
}
