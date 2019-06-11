package com.beust.kash


sealed class Command(open val background: Boolean) {
    val tokens: List<Token>
        get() = when(this) {
            is SingleCommand -> this.exec.tokens
            is AndCommands -> this.execs.flatMap { it.tokens }
            is PipeCommands -> this.execs.flatMap { it.tokens }
            is ParenCommand -> this.command.tokens
        }

    val words: List<String>
        get() = when(this) {
            is SingleCommand -> this.exec.words
            is AndCommands -> this.execs.flatMap { it.words }
            is PipeCommands -> this.execs.flatMap { it.words }
            is ParenCommand -> this.command.words
        }

    val firstWord: String
        get() = words[0]

    data class SingleCommand(val exec: Exec, override val background: Boolean = false)
        : Command(background)
    data class AndCommands(val execs: List<Exec>, override val background: Boolean = false)
        : Command(background)
    data class PipeCommands(val execs: List<Exec>, override val background: Boolean = false)
        : Command(background)
    data class ParenCommand(val command: Command, override val background: Boolean): Command(background)
}

