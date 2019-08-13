package com.beust.kash

import com.beust.kash.api.IKashContext
import com.beust.kash.api.ILineRunner
import java.io.File
import java.util.*

class KashContext constructor(engine: Engine, override val lineRunner: ILineRunner): IKashContext {
    private val kashObject = KashObject(engine)

    override val scriptPaths: List<String> get() = DotKashJsonReader.dotKash?.scriptPath ?: emptyList()
    override val paths = kashObject.paths
    override val env = kashObject.env
    override val prompt = kashObject.prompt
    override val directoryStack: Stack<String>
        get() {
            val result = kashObject.directoryStack
            if (result.isEmpty()) {
                result.push(File(".").absoluteFile.canonicalPath)
            }
            return result
        }
}