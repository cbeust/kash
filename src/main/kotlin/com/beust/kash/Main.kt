package com.beust.kash

import org.jline.terminal.TerminalBuilder
import java.io.IOException

fun main(argv: Array<String>) {
    Main().run()
}

class Main {
    fun run() {
//        System.setProperty("org.jline.terminal.dumb", "true")
        try {
            val terminal = TerminalBuilder.builder()
                    .build()
            Shell(terminal).run()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

