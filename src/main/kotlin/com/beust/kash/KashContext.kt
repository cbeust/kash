package com.beust.kash

import com.google.inject.Inject
import com.google.inject.Singleton
import java.util.*

@Singleton
class KashContext @Inject constructor(private val engine: Engine) {
    val scriptPaths: List<String> get() = DotKashJsonReader.dotKash?.scriptPath ?: emptyList()

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