package com.beust.kash

import java.util.*

class KashContext(private val engine: Engine) {
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

    val scriptPath = arrayListOf<String>()

    var prompt: String
        get() = synchronized(engine) { engine.eval("Kash.PROMPT") as String }
        set(s) { synchronized(engine) { engine.eval("Kash.PROMPT = $s") } }
}