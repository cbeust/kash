package com.beust.kash

import com.google.inject.Inject
import java.util.*

interface IKashContext {
    val directoryStack: Stack<String>
    val env: HashMap<String, String>
    val paths: ArrayList<String>
    val scriptPaths: List<String>
    val prompt: String
}

class KashContext @Inject constructor(engine: Engine): IKashContext {
    override val scriptPaths: List<String> get() = DotKashJsonReader.dotKash?.scriptPath ?: emptyList()
    override val directoryStack = engine.eval("Kash.DIRS") as Stack<String>
    override val env = engine.eval("Kash.ENV") as HashMap<String, String>
    override val paths = engine.eval("Kash.PATHS") as ArrayList<String>
    override val prompt = engine.eval("Kash.PROMPT") as String
}