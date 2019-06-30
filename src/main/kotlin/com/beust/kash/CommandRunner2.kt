package com.beust.kash

import com.beust.kash.parser.SimpleCommand
import com.beust.kash.parser.SimpleList
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CommandRunner2(private val builtins: Builtins, private val engine: Engine,
        private val commandFinder: CommandFinder, private val context: KashContext) {
    private val log = LoggerFactory.getLogger(CommandRunner2::class.java)

    fun runLine(line: String, command: SimpleList?, commandSearchResult: CommandFinder.CommandSearchResult,
            inheritIo: Boolean): CommandResult {
//        val firstWord = command.content[0]

        val result =
            if (commandSearchResult.type == CommandType.BUILT_IN) {
                // Built-in command
                log.debug("BUILT IN COMMAND")
                CommandResult(0)
//                builtins.commands[firstWord]!!(command.content)
//                    builtinCommand(command.words)
            } else if (commandSearchResult.type == CommandType.COMMAND) {
                // Shell command
//                runCommand(command, inheritIo)
                runCommand(command!!)
            } else if (commandSearchResult.type == CommandType.SCRIPT) {
                log.debug("SCRIPT")
                CommandResult(0)
//                val result = engine.eval(FileReader(File(commandSearchResult.path)),
//                        command.content.subList(1, command.content.size))
//                CommandResult(0, result?.toString())
            } else {
                throw IllegalArgumentException("Unknown command type: ${commandSearchResult.type}")
            }
        return result
    }

    private fun runCommand(list: SimpleList): CommandResult {
        var result = CommandResult(0)
        list.content.withIndex().forEach { (index, plCommand) ->
            if (plCommand.content.size == 1) {
                val command = plCommand.content[0]
                // Launch this command if either of
                // - It's the first one
                // - It's not the first one and either it's preceded by
                //     - ;
                //     - && and the previous command succeeded
                //     - || and the previous command failed
                val launch = index == 0 || (index > 0 &&
                    (
                        (plCommand.precededBy == ";") ||
                        (plCommand.precededBy == "&&" && result.returnCode == 0) ||
                        (plCommand.precededBy == "||" && result.returnCode == 1))
                    )

                // If there are more commands to run, display the output of the previous one first
                if (launch && index < list.content.size) {
                    result.display()
                }

                // Launch the command if we're good to go, in the background if there's a &
                if (launch && ! list.ampersand) {
                    // Launch immediately
                    if (command.simpleCommand != null) {
                        log.debug("Launching simpleCommand" + command.simpleCommand)
                        result = launchSimpleCommand(command.simpleCommand)
                    } else {
                        log.debug("Launching subShell " + command.subShell)
                    }
                } else if (launch) {
                    log.debug("Launching in background")
                    // Launch immediately
                    if (command.simpleCommand != null) {
                        log.debug("  Launching simpleCommand" + command.simpleCommand)
                        result = launchBackgroundCommand {
                            launchSimpleCommand(command.simpleCommand)
                        }
                    } else {
                        log.debug("  Launching subShell " + command.subShell)
                    }
                    // Launch in background
                }
            } else {
                log.debug("Launching pipeline: " + plCommand.content.joinToString(" "))
                val builders = plCommand.content.map {
                    // TOOD: handle the case where it's it.subShell and not it.simpleCommand
                    simpleCommandToProcessBuilder(it.simpleCommand)
                }
                val r = ProcessBuilder.startPipeline(builders)
                r[r.size - 1].let { lastProcess ->
                    val output = Streams.readStream(lastProcess.inputStream)
                    val error = Streams.readStream(lastProcess.errorStream)
                    result = CommandResult(0, output, error)
                }

            }
        }
        return result
    }

    private fun simpleCommandToProcessBuilder(sc: SimpleCommand): ProcessBuilder {
        return ProcessBuilder(findPath(sc.content)).also { pb ->
            if (sc.input != null) pb.redirectInput(File(sc.input))
            if (sc.output != null) pb.redirectInput(File(sc.output))
            //        if (sc.error != null) pb.redirectInput(File(sc.error))
        }
    }

    private fun launchSimpleCommand(sc: SimpleCommand): CommandResult {
        val pb = simpleCommandToProcessBuilder(sc)
        log.debug("Launching " + pb.command().joinToString(" "))

        val process = pb.start()
        val returnCode = process.waitFor()
        val stdout = Streams.readStream(process.inputStream)
        val stderr = Streams.readStream(process.errorStream)
        return CommandResult(returnCode, stdout, stderr)
    }

    private fun findPath(words: List<String>): List<String>? {
        val commandResult = commandFinder.findCommand(words[0])
        val pathCommand = commandResult?.path
        val result = listOf(pathCommand) + words.subList(1, words.size)
        val result2 = result.map { it!!.replace('/', File.separatorChar) }
        return result2
    }

    private fun launchBackgroundCommand(f: () -> CommandResult): CommandResult {
        val future = backgroundProcessesExecutor.submit<Void> {
            val commandResult = f()
            onFinish(BackgroundCommandResult(42, commandResult.returnCode))
            commandResult.display()
            null
        }
        return CommandResult(0)
    }

    private fun onFinish(result: BackgroundCommandResult) {
        log.debug("Background command completed: $result")
    }

    private val backgroundProcessesExecutor: ExecutorService = Executors.newFixedThreadPool(10)
    private val backgroundProcesses = hashMapOf<Int, BackgroundCommand>()

}
