package com.beust.kash

import com.google.inject.Inject
import com.google.inject.Singleton
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.io.Reader
import javax.script.ScriptContext
import javax.script.ScriptEngine

/**
 * Wrap a ScriptEngine and run additional things on the engine (such as running Predef.kts and
 * reading ~/.kash.kts) so that it can evaluate Kash specific values.
 */
@Singleton
class Engine @Inject constructor(private val engine: ScriptEngine) {
    private val log = LoggerFactory.getLogger(Engine::class.java)

    var lineRunner: LineRunner? = null

    init {
        //
        // Read Predef.kts
        //
        val predef = InputStreamReader(this::class.java.classLoader.getResource("kts/Predef.kts").openStream())
        engine.eval(predef)
        log.debug("Read $predef")

        //
        // Read ~/.kash.kts
        //
        File(System.getProperty("user.home"), ".kash.kts").let { dotKash ->
            if (dotKash.exists()) {
                try {
                    engine.eval(FileReader(dotKash))
                    log.debug("Read $dotKash")
                } catch (ex: Exception) {
                    System.err.println("Errors found while reading $dotKash: " + ex.message)
                }
            }
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
}
