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

typealias TokenTransform = (Token.Word, List<String>) -> List<String>

/**
 * Once a line has been read, all its tokens are passed through a list of
 * token transformers that can alter its content before it gets analyzed
 * by the shell.
 */
interface TokenTransformer {
    /**
     * Implementers of this function will typically inspect the list of strings
     * passed, while the token provides additional semantic information, such
     * as whether that token is surrounded by quotes, etc...
     */
    fun transform(token: Token.Word?, words: List<String>): List<String>
}

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
class GlobTransformer(private val directoryStack: Stack<String>) : TokenTransformer, TokenTransformer2 {
    private val log = LoggerFactory.getLogger(GlobTransformer::class.java)

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

    override fun transform(token: Token.Word?, words: List<String>): List<String> {
        val result = words.flatMap { word ->
            if (token!!.isWord && token.surroundedBy == null) {
                transform(words)
            } else {
                listOf(word)
            }
        }
        log.trace("'$words' expanded to: $result")
        return result
    }

}

/**
 * Replace lines surrounded by backticks with their evaluation by the shell.
 */
class BackTickTransformer(private val lineRunner: LineRunner): TokenTransformer, TokenTransformer2 {
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

    override fun transform(token: Token.Word?, words: List<String>): List<String> {
        val result = words.flatMap { word ->
            if (token!!.surroundedBy == "`") {
                val word = token._name.toString()
                val result = lineRunner.runLine(word, inheritIo = false)
                if (result.stdout != null) {
                    listOf(StringBuilder(result.stdout.trim()).toString())
                } else {
                    throw IllegalArgumentException(result.stderr)
                }
            } else {
                listOf(word)
            }
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
class TildeTransformer: TokenTransformer, TokenTransformer2 {
    override fun transform(words: List<String>): List<String> {
        return words.map { Tilde.expand(it) }
    }

    override fun transform(token: Token.Word?, words: List<String>): List<String> {
        val result =
            if (token!!.isWord && token.surroundedBy == null) {
                words.map {
                    Tilde.expand(it)
                }
            } else {
                words
            }
        return result
    }

}

/**
 * Replace environment variables with their value.
 */
class EnvVariableTransformer(private val env: Map<String, String>): TokenTransformer, TokenTransformer2 {
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

    override fun transform(token: Token.Word?, words: List<String>): List<String> {
        val result = words.flatMap { word ->
            var i = 0
            val finds = arrayListOf<Pair<Int, Int>>()
            while (i < word.length) {
                if (word[i] == '$') {
                    val start = i

                    if (word[i + 1] == '{') {
                        while (i < word.length && word[i] != '}') i++
                        i++
                    } else {
                        i++
                        while (i < word.length && isWord(word[i])) i++
                    }
                    finds.add(Pair(start, i))
                }
                i++
            }

            if (finds.isNotEmpty()) {
                val result = StringBuilder()
                var index = 0
                var i = 0
                while (i < finds.size) {
                    val first = finds[i].first
                    val second = finds[i].second
                    val parsedVariable = word.substring(first, second)
                    val variable = if (parsedVariable.startsWith("\${") and parsedVariable.endsWith("}"))
                        parsedVariable.substring(2, parsedVariable.length - 1)
                    else parsedVariable.substring(1)
                    result.append(word.substring(index, first))
                    val expanded = env[variable] ?: ""
                    result.append(expanded)
                    log.debug("Expanded $parsedVariable to $expanded")
                    index = second
                    i++
                }
                result.append(word.substring(index, word.length))
                listOf(result.toString())

            } else {
                listOf(word)
            }
        }
        return result

    }
}
