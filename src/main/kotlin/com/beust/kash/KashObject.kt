package com.beust.kash

import com.google.inject.Inject
import java.io.File
import java.util.*

class KashObject @Inject constructor(engine: Engine) {
    private val content = engine.eval("Kash.dump()") as Array<Any>

    private val _paths = content[0] as ArrayList<String>
    val paths: ArrayList<String>
        get() {
            val paths = _paths
            System.getenv("PATH").split(File.pathSeparator).forEach {
                paths.add(it)
            }
            return paths
        }
    val env = content[1] as HashMap<String, String>
    val prompt = content[2] as String
    val directoryStack = content[3] as Stack<String>
}