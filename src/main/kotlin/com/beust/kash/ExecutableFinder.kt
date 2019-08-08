package com.beust.kash

import com.beust.kash.api.IKashContext
import com.beust.kash.parser.SimpleCommand
import com.beust.kash.parser.SimpleList
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import kotlin.math.min

/**
 * Find executable commands on the path.
 */
class ExecutableFinder: ICommandFinder {
    private val log = LoggerFactory.getLogger(ExecutableFinder::class.java)

    /**
     * Need to introduce two implementations of this method: one for Windows and one for others.
     * For Windows, need to 1) look up commands that end with .exe, .cmd, .bat and 2) manually
     * add support for #! scripts.
     */
    override fun findCommand(word: String, list: SimpleList?, context: IKashContext): CommandFinder.CommandSearchResult? {
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
                    val path = File(c1).absolutePath
                    val lambda = {
                        runCommand(list!!, context)

                    }
                    return CommandFinder.CommandSearchResult(path, lambda)
                }
            }
        }
        return null
    }

    private fun runCommand(list: SimpleList, context: IKashContext): CommandResult {
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
                        result = launchSimpleCommand(command.simpleCommand, list, context)
                    } else {
                        log.debug("Launching subShell " + command.subShell)
                    }
                } else if (launch) {
                    log.debug("Launching in background")
                    // Launch immediately
                    if (command.simpleCommand != null) {
                        log.debug("  Launching simpleCommand" + command.simpleCommand)
                        result = Background.launchBackgroundCommand({ r: BackgroundCommandResult -> onFinish(r) }) {
                            launchSimpleCommand(command.simpleCommand, list, context)
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
                    simpleCommandToProcessBuilder(it.simpleCommand, list, context)
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

    private fun onFinish(result: BackgroundCommandResult) {
        log.debug("Background command completed: $result")
    }

    private fun findPath(words: List<String>, list: SimpleList, context: IKashContext): List<String>? {
        val commandResult = findCommand(words[0], list, context)
        val pathCommand = commandResult?._path
        val result = listOf(pathCommand) + words.subList(1, words.size)
        val result2 = result.map { it!!.replace('/', File.separatorChar) }
        return result2
    }

    private fun launchSimpleCommand(sc: SimpleCommand, list: SimpleList, context: IKashContext): CommandResult {
        fun isAlive(p: Process): Boolean {
            return try {
                p.exitValue()
                false
            } catch (e: IllegalThreadStateException) {
                true
            }
        }

        val pb = simpleCommandToProcessBuilder(sc, list, context)
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

    private fun simpleCommandToProcessBuilder(sc: SimpleCommand, list: SimpleList, context: IKashContext)
            : ProcessBuilder
    {
        return ProcessBuilder(findPath(sc.words, list, context)).also { pb ->
            if (sc.input != null) pb.redirectInput(File(sc.input))
            if (sc.output != null) pb.redirectOutput(File(sc.output))
            pb.directory(File(context.directoryStack.peek()))
//        if (sc.error != null) pb.redirectInput(File(sc.error))
        }
    }


}