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
    private val kashObject = KashObject(engine)

    override val scriptPaths: List<String> get() = DotKashJsonReader.dotKash?.scriptPath ?: emptyList()
    override val paths = kashObject.paths
    override val env = kashObject.env
    override val prompt = kashObject.prompt
    override val directoryStack = kashObject.directoryStack
}