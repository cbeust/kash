package com.beust.kash

import com.beust.kash.parser.SimpleCmd
import com.beust.kash.parser.SimpleCommand
import com.beust.kash.word.KashWordParser
import java.io.File
import java.io.FileFilter
import java.io.StringReader
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.util.*

/**
 * Token transformers replace words found on the command line (e.g. $VAR, ~, *.kt, etc...).
 */
interface TokenTransformer {
    fun shouldTransform(command: SimpleCmd) = command.surroundedBy == null

    fun transform(words: List<String>): List<String>

    fun transform(command: SimpleCommand, words: List<SimpleCmd>): List<SimpleCmd> {
        return command.content.map {  cmd ->
            val tWords =
                if (shouldTransform(cmd)) {
                    transform(cmd.content)
                } else {
                    cmd.content
                }
            SimpleCmd(tWords, cmd.surroundedBy)
        }
    }
}

/**
 * Expand glob patterns (*, ?, etc...).
 */
class GlobTransformer(private val directoryStack: Stack<String>) : TokenTransformer {
    @Suppress("PrivatePropertyName")
    private val GLOB_CHARACTERS = "*?[]".toSet()

    /**
     * Only transform this command if one of the words contains a glob character.
     */
    override fun shouldTransform(command: SimpleCmd) : Boolean {
        return command.content.any { it.toSet().intersect(GLOB_CHARACTERS).isNotEmpty() }
    }

    override fun transform(words: List<String>): List<String> {
        val result = words.flatMap { word ->
            val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:$word")
            val dir = directoryStack.peek()
            val files = File(dir).listFiles(FileFilter {
                pathMatcher.matches(Paths.get(it.name))
            })
            if (files == null || files.isEmpty()) listOf(word)
            else files.map { it.name }
        }
        return result
    }
}

/**
 * Replace lines surrounded by backticks with their evaluation by the shell.
 */
class BackTickTransformer(private val lineRunner: LineRunner): TokenTransformer {
    override fun shouldTransform(command: SimpleCmd) = command.surroundedBy == "`"

    override fun transform(words: List<String>): List<String> {
        val r = lineRunner.runLine(words.joinToString(" "), inheritIo = false)
        val result =
            if (r.stdout != null) {
                listOf(r.stdout.trim())
            } else {
                throw IllegalArgumentException(r.stderr)
            }
        return result
    }
}

object Tilde {
    private val homeDir = System.getProperty("user.home")

    fun expand(s: String) = s.replace("~", homeDir)
}

/**
 * Replace occurrences of ~ with the user home dir.
 */
class TildeTransformer: TokenTransformer {
    override fun transform(words: List<String>): List<String> {
        return words.map { Tilde.expand(it) }
    }
}

/**
 * Replace environment variables with their value.
 */
class EnvVariableTransformer(private val env: Map<String, String>): TokenTransformer {
    override fun transform(words: List<String>): List<String> = words.map { word ->
        val fragments = KashWordParser(StringReader(word)).ParsedWord()
        val result = fragments.map { fragment ->
            if (fragment.isWord) fragment.word
            else env[fragment.word] ?: ""
        }
        result.joinToString("")
    }
}
