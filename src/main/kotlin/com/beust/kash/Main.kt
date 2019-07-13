package com.beust.kash

import com.google.inject.Guice
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import java.io.IOException

fun main(argv: Array<String>) {
    Main().run()
}

class Main {
    fun run() {
        System.setProperty("org.jline.terminal.dumb", "true")
        setIdeaIoUseFallback()
        try {
            val injector = Guice.createInjector(KashModule())
            val engine = injector.getInstance(Engine::class.java)
            val shell = injector.getInstance(Shell::class.java)
            engine.lineRunner = shell
            shell.run()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

