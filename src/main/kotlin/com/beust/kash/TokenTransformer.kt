package com.beust.kash

import com.beust.kash.parser.SimpleCmd
import com.beust.kash.parser.SimpleCommand
import com.beust.kash.word.KashWordParser
import org.slf4j.LoggerFactory
import java.io.File
import java.io.StringReader
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.util.*

interface TokenTransformer2 {
    fun transform(command: SimpleCommand, words: List<SimpleCmd>): List<SimpleCmd> {
        val result = command.content.map {  cmd ->
            val tWords =
                if (shouldTransform(cmd)) {
                    transform(cmd.content)
                } else {
                    cmd.content
                }
            SimpleCmd(tWords, cmd.surroundedBy)
        }
        return result
    }

    fun shouldTransform(command: SimpleCmd) = command.surroundedBy == null

    fun transform(words: List<String>): List<String>
}

/**
 * Expand glob patterns (*, ?, etc...).
 */
class GlobTransformer(private val directoryStack: Stack<String>) : TokenTransformer2 {
    @Suppress("PrivatePropertyName")
    private val GLOB_CHARACTERS = "*?[]".toSet()

    /**
     * Only transform this command if one of the words contains a glob character.
     */
    override fun shouldTransform(command: SimpleCmd) : Boolean {
        return command.content.any { it.toSet().intersect(GLOB_CHARACTERS).isEmpty() }
    }

    override fun transform(words: List<String>): List<String> {
        val result = words.flatMap { word ->
            val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:$word")
            val matches = File(directoryStack.peek()).listFiles().filter {
                pathMatcher.matches(Paths.get(it.name))
            }
            if (matches.isEmpty()) listOf(word)
            else matches.map { it.name }
        }
        return result
    }
}

/**
 * Replace lines surrounded by backticks with their evaluation by the shell.
 */
class BackTickTransformer(private val lineRunner: LineRunner): TokenTransformer2 {
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
class TildeTransformer: TokenTransformer2 {
    override fun transform(words: List<String>): List<String> {
        return words.map { Tilde.expand(it) }
    }
}

/**
 * Replace environment variables with their value.
 */
class EnvVariableTransformer(private val env: Map<String, String>): TokenTransformer2 {
    private val log = LoggerFactory.getLogger(EnvVariableTransformer::class.java)

    private fun isWord(c: Char) = true

    override fun transform(words: List<String>): List<String> {
        val result = words.map { word ->
            val fragments = KashWordParser(StringReader(word)).ParsedWord()
            val r = fragments.map { fragment ->
                if (fragment.isWord) fragment.word
                    else env[fragment.word] ?: ""
            }
            r.joinToString("")
        }

        return result
    }
}
