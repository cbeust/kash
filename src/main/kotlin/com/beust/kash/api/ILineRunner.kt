package com.beust.kash.api

interface ILineRunner {
    fun runLine(line: String, context: IKashContext, inheritIo: Boolean): CommandResult
}
