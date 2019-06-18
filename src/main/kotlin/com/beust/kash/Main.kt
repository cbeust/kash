package com.beust.kash

import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jline.terminal.TerminalBuilder
import java.io.IOException

fun main(argv: Array<String>) {
    Main().run()
}

class Main {
    fun run() {
        System.setProperty("org.jline.terminal.dumb", "true")
        setIdeaIoUseFallback()
        try {
            val terminal = TerminalBuilder.builder()
                    .build()
            Shell2(terminal).run()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

