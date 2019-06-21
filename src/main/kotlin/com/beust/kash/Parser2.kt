package com.beust.kash

import me.sargunvohra.lib.cakeparse.api.*

fun main() {
    val idTransformer: TokenTransform = { word: Token.Word, s: List<String> -> s }
    Parser2(idTransformer).parse("echo a && ls -l && wc")
}

class Parser2(private val transform: TokenTransform) {
    fun parse(input: String): Command? {
        val space = token("space", "[ \\t\\r]+", ignore = true)
        val word = token("word", "[-$.~/=*?a-zA-Z0-9]+")
        val ampersand = token("ampersand", "&")
        val andAnd = token("andAnd", "&&")
        val singleCommand = oneOrMore(word)
        val andCommand = singleCommand and zeroOrMore(andAnd and singleCommand)
        val goal1 = singleCommand and optional(ampersand)
        val goal = andCommand

        val tokens = setOf(space, word, andAnd, ampersand)
        val result =
            try {
                val r = tokens.lexer().lex(input).parseToEnd(goal)
                val rv = r.value
                val background = rv.second != null
                val tokens = rv.first.map { Token.Word(StringBuilder(it.raw)) }
                Command.SingleCommand(Exec.toExec(tokens, transform), background)
            } catch (e: Exception) {
                System.err.println("Couldn't parse: " + e.message)
                null
            }
        return result
    }

    fun f1() {
        val lPar = token("lPar", "\\(")
        val rPar = token("rPar", "\\)")
        val number = token("number", "[0-9]+")
        val expr = number
        val plus = token("plus", "\\+")
        val space = token("space", "[ \\t\\r]+", ignore = true)

        val parenExpr = lPar then expr before rPar
        val primExpr = number map { it.raw.toInt() } or parenExpr
        val goal = oneOrMore(primExpr)

        fun run() {
            val tokens = setOf(lPar, rPar, number, expr, plus, space)
            val tokens2 = setOf(space, lPar, rPar, number)
            try {
                val input = "(23)  42     43"
                val result = tokens2.lexer().lex(input).parseToEnd(goal).value
                println("Result: $result")
            } catch (e: Exception) {
                e.printStackTrace()
                System.err.println("Lexing error: ${e.message}")
            }
        }
    }
}