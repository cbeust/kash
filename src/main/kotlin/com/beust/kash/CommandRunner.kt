package com.beust.kash

import com.beust.kash.parser.SimpleCommand
import com.beust.kash.parser.SimpleList
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import kotlin.math.min


class CommandRunner(private val builtins: Builtins, private val engine: Engine,
        private val commandFinder: CommandFinder, private val context: IKashContext) {
    private val log = LoggerFactory.getLogger(CommandRunner::class.java)

    fun runLine(line: String, command: SimpleList?, commandSearchResult: CommandFinder.CommandSearchResult,
            inheritIo: Boolean): CommandResult {
//        val firstWord = command.content[0]

        val words = line.split(" ", "\t")
        val result = when {
            commandSearchResult.type == CommandType.BUILT_IN -> {
                // Built-in command
                log.debug("Detected built-in command")
                builtins.commands[commandSearchResult.path]!!(words)
            }
            commandSearchResult.type == CommandType.COMMAND -> {
                // Shell command
                log.debug("Detected command")
                runCommand(command!!)
            }
            commandSearchResult.type == CommandType.SCRIPT -> {
                // Script
                log.debug("Detected script")
                val result = engine.eval(FileReader(File(commandSearchResult.path)),
                        words.subList(1, words.size))
                CommandResult(0, result?.toString())
            }
            else -> throw IllegalArgumentException("Unknown command type: ${commandSearchResult.type}")
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
                        result = Background.launchBackgroundCommand( { r: BackgroundCommandResult -> onFinish(r)}) {
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
                r[r.size - 1].let { lastProcess: Process ->
                    val output = Streams.readStream(lastProcess.inputStream)
                    val error = Streams.readStream(lastProcess.errorStream)
                    result = CommandResult(0, output, error)
                }

            }
        }
        return result
    }

    private fun simpleCommandToProcessBuilder(sc: SimpleCommand): ProcessBuilder {
        return ProcessBuilder(findPath(sc.words)).also { pb ->
            if (sc.input != null) pb.redirectInput(File(sc.input))
            if (sc.output != null) pb.redirectOutput(File(sc.output))
            pb.directory(File(context.directoryStack.peek()))
//        if (sc.error != null) pb.redirectInput(File(sc.error))
        }
    }

    private fun launchSimpleCommand(sc: SimpleCommand): CommandResult {
        fun isAlive(p: Process): Boolean {
            return try {
                p.exitValue()
                false
            } catch (e: IllegalThreadStateException) {
                true
            }
        }

        val pb = simpleCommandToProcessBuilder(sc)
        log.debug("Launching " + pb.command().joinToString(" "))

        val process = pb.start()

        val out = process.inputStream
        val inStream = process.outputStream

        val buffer = ByteArray(4000)
        while (isAlive(process)) {
            val no = out.available()
            if (no > 0) {
                val n = out.read(buffer, 0, min(no, buffer.size))
                println(String(buffer, 0, n))
            }

            val ni = System.`in`.available()
            if (ni > 0) {
                val n = System.`in`.read(buffer, 0, min(ni, buffer.size))
                inStream.write(buffer, 0, n)
                inStream.flush()
            }

            try {
                Thread.sleep(10)
            } catch (e: InterruptedException) {
            }

        }

        val returnCode = process.waitFor()
        val stdout = Streams.readStream(process.inputStream)
        val stderr = Streams.readStream(process.errorStream)
        return CommandResult(returnCode, stdout, stderr)
    }

    private fun findPath(words: List<String>): List<String>? {
        val commandResult = commandFinder.findCommand(words[0], context)
        val pathCommand = commandResult?.path
        val result = listOf(pathCommand) + words.subList(1, words.size)
        val result2 = result.map { it!!.replace('/', File.separatorChar) }
        return result2
    }

    private fun onFinish(result: BackgroundCommandResult) {
        log.debug("Background command completed: $result")
    }
}
