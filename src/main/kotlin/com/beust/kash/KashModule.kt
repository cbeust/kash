package com.beust.kash

import com.google.inject.AbstractModule
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class KashModule() : AbstractModule() {
    private val log = LoggerFactory.getLogger(KashModule::class.java)

    override fun configure() {
        bind(Terminal::class.java).toInstance(TerminalBuilder.builder().build())

        val scriptEngine = ScriptEngineManager().getEngineByExtension("kash.kts")
                ?: throw IllegalArgumentException("Couldn't find a script engine for .kash.kts")

        val engine = Engine(scriptEngine)
        bind(ScriptEngine::class.java).toInstance(scriptEngine)
        bind(Engine::class.java).toInstance(engine)
    }
}