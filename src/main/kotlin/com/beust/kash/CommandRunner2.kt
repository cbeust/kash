package com.beust.kash

import com.beust.kash.parser.KashParser
import org.slf4j.LoggerFactory
import java.io.File

class CommandRunner2(private val builtins: Builtins, private val engine: Engine,
        private val commandFinder: CommandFinder, private val context: KashContext) {
    private val log = LoggerFactory.getLogger(CommandRunner::class.java)

    fun runLine(line: String, command: KashParser.SimpleList?, commandSearchResult: CommandFinder.CommandSearchResult,
            inheritIo: Boolean): CommandResult {
//        val firstWord = command.content[0]

        val result =
            if (commandSearchResult.type == CommandType.BUILT_IN) {
                // Built-in command
                println("BUILT IN COMMAND")
                CommandResult(0)
//                builtins.commands[firstWord]!!(command.content)
//                    builtinCommand(command.words)
            } else if (commandSearchResult.type == CommandType.COMMAND) {
                // Shell command
//                runCommand(command, inheritIo)
                runCommand(command!!)
            } else if (commandSearchResult.type == CommandType.SCRIPT) {
                println("SCRIPT")
                CommandResult(0)
//                val result = engine.eval(FileReader(File(commandSearchResult.path)),
//                        command.content.subList(1, command.content.size))
//                CommandResult(0, result?.toString())
            } else {
                throw IllegalArgumentException("Unknown command type: ${commandSearchResult.type}")
            }
        return result
    }

    private fun runCommand(list: KashParser.SimpleList): CommandResult {
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
                        println("Launching simpleCommand" + command.simpleCommand)
                        result = launchSimpleCommand(command.simpleCommand)
                    } else {
                        println("Launching subShell " + command.subShell)
                    }
                } else if (launch) {
                    println("Launching in background")
                    // Launch in background
                }
            } else {
                println("Launching pipeline: " + plCommand.content.joinToString(" "))
            }
        }
        return result
    }

    private fun launchSimpleCommand(sc: KashParser.SimpleCommand): CommandResult {
        val pb = ProcessBuilder(findPath(sc.content))
        if (sc.input != null) pb.redirectInput(File(sc.input))
        if (sc.output != null) pb.redirectInput(File(sc.output))
//        if (sc.error != null) pb.redirectInput(File(sc.error))

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

}
