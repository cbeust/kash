package com.beust.kash

data class Exec(private val passedTokens: List<Token>,
        val input: String? = null, val output: String? = null, val error: String?,
        val transform: TokenTransform) {
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
