package com.beust.kash

import com.beust.kash.Parser.Parser.isWord
import org.slf4j.LoggerFactory
import java.io.File
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
    fun transform(token: Token.Word, words: List<String>): List<String>
}

/**
 * Expand glob patterns (*, ?, etc...).
 */
class GlobTransformer(private val directoryStack: Stack<String>) : TokenTransformer {
    private val log = LoggerFactory.getLogger(GlobTransformer::class.java)

    @Suppress("PrivatePropertyName")
    private val GLOB_CHARACTERS = "*?[]".toSet()

    override fun transform(token: Token.Word, words: List<String>): List<String> {
        val result = words.flatMap { word ->
            if (token.isWord && token.surroundedBy == null) {
                if (word.toSet().intersect(GLOB_CHARACTERS).isEmpty()) {
                    listOf(word)
                } else {
                    val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:$word")
                    val matches = File(directoryStack.peek()).listFiles().filter {
                        pathMatcher.matches(Paths.get(it.name))
                    }
                    val result =
                        if (matches.isEmpty()) emptyList()
                        else matches.map { it.name }
                    result
                }

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
class BackTickTransformer(private val commandRunner: CommandRunner): TokenTransformer {
    override fun transform(token: Token.Word, words: List<String>): List<String> {
        val result = words.flatMap { word ->
            if (token.surroundedBy == "`") {
                val result = commandRunner.runLine(token._name.toString(), inheritIo = false)
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
class TildeTransformer: TokenTransformer {
    val homeDir = System.getProperty("user.home")

    override fun transform(token: Token.Word, words: List<String>): List<String> {
        val result =
            if (token.isWord && token.surroundedBy == null) {
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
class EnvVariableTransformer(private val env: Map<String, String>): TokenTransformer {
    private val log = LoggerFactory.getLogger(EnvVariableTransformer::class.java)

    override fun transform(token: Token.Word, words: List<String>): List<String> {
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
