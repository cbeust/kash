package com.beust.kash

data class Exec(private val passedTokens: List<Token>,
        val input: String? = null, val output: String? = null, val error: String?,
        val transform: TokenTransform) {

    companion object {
        fun toExec(tokens: List<Token>, transform: TokenTransform): Exec {
            var i = 0
            var input: String? = null
            var output: String? = null
            var error: String? = null
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
                } else if (tokens[i] is Token.TwoGreater) {
                    if (i + 1 < tokens.size) {
                        error = (tokens[i + 1] as Token.Word).name[0]
                        i++
                    }
                }
                i++
            }
            return Exec(tokens, input, output, error, transform)
        }
    }

    val tokens = passedTokens.map {
        val result =
            if (it is Token.Word) {
                it.name = transform(it, it.name)
                it
            } else it
        result

    }
    val words = tokens.flatMap { if (it is Token.Word) it.name else listOf(it.toString()) }
    override fun toString() = "Exec($tokens)"
}
