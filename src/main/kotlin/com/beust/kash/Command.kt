package com.beust.kash


sealed class Command {
    val tokens: List<Token>
        get() = when(this) {
            is SingleCommand -> this.exec.tokens
            is AndCommands -> this.execs[0].tokens
            is PipeCommands -> this.execs[0].tokens
            else -> emptyList()
        }

    val words: List<String>
        get() = when(this) {
                is SingleCommand -> this.exec.words
                is AndCommands -> this.execs[0].words
                is PipeCommands -> this.execs[0].words
                else -> listOf("")
        }

    val firstWord: String
        get() = words[0]

    data class SingleCommand(val exec: Exec) : Command()
    data class AndCommands(val execs: List<Exec>) : Command()
    data class PipeCommands(val execs: List<Exec>): Command()
}

