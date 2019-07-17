package com.beust.kash

interface LineRunner {
    fun runLine(line: String, context: IKashContext, inheritIo: Boolean): CommandResult
}
