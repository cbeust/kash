package com.beust.kash

//val idTransformer: (Token.Word, String) -> String
//        = {word: Token.Word, s: String -> s }


class Parser(private val transform: TokenTransform) {

    companion object Parser {
        val wordCharacters = setOf('-', '$', '.', '~', '/', '=', '*', '?', '(', ')', '_')
        fun isWord(c: Char) = c.isLetterOrDigit() || wordCharacters.contains(c)
    }

    fun lexicalParse(line: String): List<Token> {

        val result = arrayListOf<Token>()
        var currentWord: StringBuilder? = null
        var index = 0
        while (index < line.length) {
            val c = line[index]
            fun processQuote(quote: Char) {
                index++
                val start = index
                while (index < line.length && line[index] != quote) index++
                val word = StringBuilder(line.substring(start, index))
                result.add(Token.Word(word, quote.toString()))
                index++
            }

            if (c == '\'' || c == '\"' || c == '`') processQuote(c)

            if (isWord(c)) {
                if (currentWord == null) {
                    currentWord = StringBuilder().append(c)
                    result.add(Token.Word(currentWord!!))
                } else {
                    currentWord.append(c)
                }
            } else {
                currentWord = null
                if (c == ' ' || c == '\t') {
                    // ignore
                } else if (c == ';') {
                    result.add(Token.SemiColon())
                } else if (c == '|') {
                    result.add(Token.Pipe())
                } else if (c == '&') {
                    if (index < line.length - 1 && line[index + 1] == '&') {
                        result.add(Token.AndAnd())
                        index++ // skip next
                    } else {
                        result.add(Token.And())
                    }
                } else if (c == '>') {
                    if (index < line.length - 1 && line[index + 1] == '>') {
                        result.add(Token.GreaterGreater())
                        index++ // skip next
                    } else {
                        result.add(Token.Greater())
                    }
                } else if (c == '<') {
                    result.add(Token.Less())
                }
            }
            index++
        }

        return result
    }

    fun parse(line: String): List<Command> {
        val result = arrayListOf<Command>()
        val tokens = lexicalParse(line)
        var index = 0
        val pipeList = arrayListOf<List<Token>>()
        val andList = arrayListOf<List<Token>>()

        while (index < tokens.size) {
            val current = arrayListOf<Token>()

            fun flushInput() {
                if (pipeList.isNotEmpty()) {
                    pipeList.add(current)
                    result.add(buildPipeList(pipeList))
                    pipeList.clear()
                } else if (andList.isNotEmpty()) {
                    andList.add(current)
                    result.add(buildAndList(andList))
                    andList.clear()
                } else {
                    result.add(buildCommand(current))
                }
            }

            while (index < tokens.size && ! tokens[index].isWord) index++
            while (index < tokens.size && ! tokens[index].isSeparator) {
                current.add(tokens[index])
                index++
            }
            if (index < tokens.size) when (tokens[index]) {
                is Token.Pipe -> pipeList.add(current)
                is Token.AndAnd -> andList.add(current)
                else -> {
                    flushInput()
                }
            } else {
                flushInput()
            }
            index++
        }
        return result
    }

    private fun buildAndList(andList: List<List<Token>>) = Command.AndCommands(andList.map { toExec(it) })

    private fun buildPipeList(pipeList: List<List<Token>>) = Command.PipeCommands(pipeList.map { toExec(it) })

    private fun toExec(tokens: List<Token>): Exec {
        var i = 0
        val words = arrayListOf<String>()
        var input: String? = null
        var output: String? = null
        while (i < tokens.size) {
            if (tokens[i] is Token.Less) {
                if (i + 1 < tokens.size) {
                    input = (tokens[i + 1] as Token.Word).name[0]
                    i++
                }
            } else if (tokens[i] is Token.Greater) {
                if (i + 1 < tokens.size) {
                    output = (tokens[i + 1] as Token.Word).name[0]
                    i++
                }
            }
            i++
        }
        return Exec(tokens, input, output, transform)
    }

    private fun buildCommand(tokens: List<Token>) = Command.SingleCommand(toExec(tokens))
}
