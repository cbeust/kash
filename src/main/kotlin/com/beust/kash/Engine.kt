package com.beust.kash

import com.google.inject.Inject
import com.google.inject.Singleton
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.io.Reader
import java.util.*
import javax.script.ScriptContext
import javax.script.ScriptEngine

@Singleton
class Engine @Inject constructor(private val engine: ScriptEngine) {
    private val log = LoggerFactory.getLogger(Engine::class.java)

    var lineRunner: LineRunner? = null

    init {
        val PREDEF = "kts/Predef.kts"

        //
        // Read Predef
        //
        val predef = InputStreamReader(this::class.java.classLoader.getResource(PREDEF).openStream())
        engine.eval(predef)
        log.debug("Read $PREDEF")

        val DOT_KASH_KTS = File(System.getProperty("user.home"), ".kash.kts")
        //
        // Read ~/.kash.kts
        //
        if (DOT_KASH_KTS.exists()) {
            try {
                engine.eval(FileReader(DOT_KASH_KTS))
                log.debug("Read $DOT_KASH_KTS")
            } catch(ex: Exception) {
                System.err.println("Errors found while reading $DOT_KASH_KTS: " + ex.message)
            }
        }

        //
        // Copy the path
        //
        System.getenv("PATH").split(File.pathSeparator).forEach {
            paths.add(it)
        }
    }

    companion object Engine {
        const val ARGS = "args"
        const val LINE_RUNNER = "lineRunner"
    }

    private fun setUpBindings(args: List<String> = emptyList()) {
        // Temporary hack that should be removed when 1.3.50 comes out
        engine.getBindings(ScriptContext.ENGINE_SCOPE)[ARGS] = args
        engine.getBindings(ScriptContext.ENGINE_SCOPE)[LINE_RUNNER] = lineRunner
    }

    fun eval(script: Reader, args: List<String> = emptyList()) = setUpBindings(args).also {
        engine.eval(script)
    }

    fun eval(script: String): Any? {
        setUpBindings()
        try {
            return engine.eval(script)
        } catch(ex: Exception) {
            System.err.println("Couldn't evaluate $script: " + ex.message)
            throw ex
        }
    }

    val directoryStack: Stack<String>
        get() = synchronized(engine) {
            return engine.eval("Kash.DIRS") as Stack<String>
        }

    val env: HashMap<String, String>
        get() = synchronized(engine) {
            return engine.eval("Kash.ENV") as HashMap<String, String>
        }

    val paths: ArrayList<String>
        get() = synchronized(engine) {
            return engine.eval("Kash.PATHS") as ArrayList<String>
        }

    var prompt: String
        get() = synchronized(engine) { engine.eval("Kash.PROMPT") as String }
        set(s) { synchronized(engine) { engine.eval("Kash.PROMPT = $s") } }

}
