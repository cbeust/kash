package com.beust.kash

import com.beust.kash.api.CommandResult
import com.beust.kash.api.IKashContext

interface LineRunner {
    fun runLine(line: String, context: IKashContext, inheritIo: Boolean): CommandResult
}
