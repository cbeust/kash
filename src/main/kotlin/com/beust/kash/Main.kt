package com.beust.kash

import com.google.inject.AbstractModule
import com.google.inject.Guice
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStreamReader
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

fun main(argv: Array<String>) {
    Main().run()
}

class Main {
    private val log = LoggerFactory.getLogger(Main::class.java)


    fun run() {
        System.setProperty("org.jline.terminal.dumb", "true")
        setIdeaIoUseFallback()
        try {
            val injector = Guice.createInjector(KashModule())
            val engine = injector.getInstance(Engine::class.java)
            val shell = injector.getInstance(Shell2::class.java)
            engine.lineRunner = shell
            shell.run()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class KashModule() : AbstractModule() {
    private val log = LoggerFactory.getLogger(KashModule::class.java)
    private val PREDEF = "kts/Predef.kts"

    override fun configure() {
        val scriptEngine = ScriptEngineManager().getEngineByExtension("kash.kts")
                ?: throw IllegalArgumentException("Couldn't find a script engine for .kash.kts")

        bind(Terminal::class.java).toInstance(TerminalBuilder.builder().build())
        val engine = Engine(scriptEngine)

        //
        // Read Predef
        //
        val predef = InputStreamReader(this::class.java.classLoader.getResource(PREDEF).openStream())
        engine.eval(predef)
        log.debug("Read $PREDEF")

        bind(ScriptEngine::class.java).toInstance(scriptEngine)
        bind(Engine::class.java).toInstance(engine)
        val context = KashContext(engine)
        DotKashJsonReader.dotKash?.scriptPath?.let {
            context.scriptPath.addAll(it)
        }
        bind(KashContext::class.java).toInstance(context)
        bind(ExecutableFinder::class.java).toInstance(ExecutableFinder(context.paths))
        bind(ScriptFinder::class.java).toInstance(ScriptFinder(context.scriptPath))
    }
}