package com.beust.kash

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.google.inject.Guice
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import java.io.BufferedInputStream
import java.io.IOException

class Args {
    @Parameter(names = [ "--ping"])
    var ping: Boolean = false
}

fun main(argv: Array<String>) {
    val args = Args()
    JCommander.newBuilder().addObject(args).build().parse(*argv)
    if (args.ping) {
        println("pong")
    } else {
        Main().run()
    }
}

class Main {
    fun run() {
        val versionStream = this::class.java.classLoader.getResource("version.txt").openStream()
        val version = BufferedInputStream(versionStream).bufferedReader().readLine()
        print("""  _  __        _   _     
 | |/ / __ _  | | | |__  
 | ' / / _` |/ __)| '_ \ 
 | . \| (_| |\__ \| | | |
 |_|\_\\__,_|(   /|_| |_| $version
              |_|


        """.trimIndent())
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

