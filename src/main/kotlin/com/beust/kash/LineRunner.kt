package com.beust.kash

interface LineRunner {
    fun runLine(line: String, inheritIo: Boolean): CommandResult
}
