package com.beust.kash

import com.google.inject.Inject
import com.google.inject.Singleton
import java.io.Reader
import java.util.*
import javax.script.ScriptContext
import javax.script.ScriptEngine

@Singleton
class Engine @Inject constructor(private val engine: ScriptEngine) {
    var lineRunner: LineRunner? = null

    companion object Engine {
        const val ARGS = "args"
        const val LINE_RUNNER = "lineRunner"
    }

    private fun setUpBindings(args: List<String> = emptyList()) {
        // Temporary hack that should be removed when 1.3.50 comes out
        engine.getBindings(ScriptContext.ENGINE_SCOPE)[ARGS] = args
        engine.getBindings(ScriptContext.ENGINE_SCOPE)[LINE_RUNNER] = lineRunner
    }

    fun eval(script: Reader, args: List<String> = emptyList()): Any? {
        setUpBindings(args)
        val result = engine.eval(script)
        return result
    }

    fun eval(script: String): Any? {
        setUpBindings()
        return engine.eval(script)
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
