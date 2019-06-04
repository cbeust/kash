package com.beust.kash

import org.jline.terminal.TerminalBuilder
import java.io.IOException

fun main(argv: Array<String>) {
    Main().run()
}

class Main {
    fun run() {
        System.setProperty("org.jline.terminal.dumb", "true")
        try {
            val terminal = TerminalBuilder.builder()
                    .build()
            Shell(terminal).run()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
//        try {
//            TerminalFactory.get().restore()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }

        }
    }


}

//
//fun script() {
//    with(engine) {
//        eval("val x = 3")
//        val res2 = eval("x + 2")
//
//        println("Result: $res2")
//    }
//}