package com.beust.kash

import com.beust.kash.api.Builtin
import com.beust.kash.api.CommandResult
import com.beust.kash.api.IKashContext

class InternalBuiltin {
    @Builtin
    fun khello(words: List<String>, context: IKashContext): CommandResult {
        println("line runner: " + context.lineRunner)
        val result = context.lineRunner.runLine("ls", context, true)
        result?.display()
        println("Hello " + (if (words.size > 1) words[1] else "stranger"))
        return CommandResult(0)
    }
}
