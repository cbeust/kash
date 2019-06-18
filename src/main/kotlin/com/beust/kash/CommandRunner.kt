package com.beust.kash

import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CommandRunner(private val builtins: Builtins, private val engine: Engine,
        private val commandFinder: CommandFinder, private val context: KashContext) {
    private val log = LoggerFactory.getLogger(CommandRunner::class.java)

    fun runLine(line: String, command: Command, commandSearchResult: CommandFinder.CommandSearchResult,
            inheritIo: Boolean): CommandResult? {
        val firstWord = command.firstWord


        val result =
            if (commandSearchResult?.type == CommandType.BUILT_IN) {
                // Built-in command
                builtins.commands[firstWord]!!(command.words)
//                    builtinCommand(command.words)
            } else if (commandSearchResult?.type == CommandType.COMMAND) {
                // Shell command
                runCommand(command, inheritIo)
            } else if (commandSearchResult?.type == CommandType.SCRIPT) {
                val result = engine.eval(FileReader(File(commandSearchResult.path)),
                        command.words.subList(1, command.words.size))
                CommandResult(0, result?.toString() ?: null)
            } else {
                null
            }
        return result
    }

    private fun runCommand(command: Command, inheritIo: Boolean): CommandResult {
        val result: CommandResult = when (command) {
            is Command.ParenCommand -> {
                if (command.background) {
                    launchBackgroundCommand {
                        runCommand(command.command, inheritIo)
                    }
                } else {
                    runCommand(command.command, inheritIo)
                }
            }
            is Command.SingleCommand -> {
                // A single command
                if (command.background) {
                    launchBackgroundCommand {
                        launchCommand(command.exec, true)
                    }
                } else {
                    launchCommand(command.exec, inheritIo)
                }
            }
            is Command.PipeCommands -> {
                // Commands piped between each other
                val builders = command.execs.map { exec ->
                    execToProcessBuilder(exec, inheritIo = false)
                }
                val r = ProcessBuilder.startPipeline(builders)
                r[r.size - 1].let { lastProcess ->
                    val output = Streams.readStream(lastProcess.inputStream)
                    val error = Streams.readStream(lastProcess.errorStream)
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
                    val path = commandFinder.findCommand(exec.words[0])?.path
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
            exec.error?.let {
                redirect = true
                result.redirectError(File(it))
            }
            if (! context.directoryStack.isEmpty()) {
                result.directory(File(context.directoryStack.peek()))
            }
            // Copy the native shell environment into Kash before launching the process
            val pbEnv = result.environment()
            context.env.entries.forEach { entry ->
                pbEnv[entry.key] = entry.value
            }
            if (inheritIo && ! redirect) result.inheritIO()
            return result
        }
    }

    private fun launchBackgroundCommand(f: () -> CommandResult): CommandResult {
        val future = backgroundProcessesExecutor.submit<Void> {
            val commandResult = f()
            onFinish(BackgroundCommandResult(42, commandResult.returnCode))
            null
        }
        return CommandResult(0)
    }

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

            } else {
                newTokens.add(w)
            }
            i++
        }

        if (newTokens.isEmpty()) {
            //
            // Just defining new environment variables (e.g. "A=B"), add them to the global environment
            //
            commandEnv.entries.forEach {
                context.env[it.key] = it.value
                log.debug("Defined ${it.key}=${it.value}")
            }
            return CommandResult(0)
        } else {
            //
            // Defining environment variables and launching a new process, e.g. "A=B ls"
            // Add this environment only to this process launch, not to the global environment
            //

            val newExec = Exec(newTokens, exec.input, exec.output, exec.error, exec.transform)
            val pb = execToProcessBuilder(newExec, inheritIo)
            commandEnv.entries.forEach {
                pb.environment()[it.key] = it.value
                log.debug("Defined locally ${it.key}=${it.value}")
            }

            log.debug("Launching " + pb.command().joinToString(" "))

            val process = pb.start()
            val returnCode = process.waitFor()
            val stdout = Streams.readStream(process.inputStream)
            val stderr = Streams.readStream(process.errorStream)
            return CommandResult(returnCode, stdout, stderr)
        }
    }

    private fun findPath(words: List<Token>): List<String>? {
        val commandResult = commandFinder.findCommand((words[0] as Token.Word).name[0])
        val pathCommand = commandResult?.path
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

    fun onFinish(result: BackgroundCommandResult) {
        println("Background command completed: $result")
    }

    private val backgroundProcessesExecutor: ExecutorService = Executors.newFixedThreadPool(10)
    private val backgroundProcesses = hashMapOf<Int, BackgroundCommand>()
}