package com.beust.kash

import java.io.InputStreamReader
import java.util.*
import javax.script.ScriptContext
import javax.script.ScriptEngine

class Engine(private val engine: ScriptEngine) {
    companion object Engine {
        const val ARGS = "args"
    }

    fun eval(script: InputStreamReader, args: List<String> = emptyList()): Any? {
        // Temporary hack that should be removed when 1.3.50 comes out
        engine.getBindings(ScriptContext.ENGINE_SCOPE)[ARGS] = args
        return engine.eval(script)
    }

    fun eval(script: String): Any? {
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
