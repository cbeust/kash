package com.beust.kash

import com.beust.kash.api.IKashContext
import com.google.inject.Inject

class KashContext @Inject constructor(engine: Engine): IKashContext {
    private val kashObject = KashObject(engine)

    override val scriptPaths: List<String> get() = DotKashJsonReader.dotKash?.scriptPath ?: emptyList()
    override val paths = kashObject.paths
    override val env = kashObject.env
    override val prompt = kashObject.prompt
    override val directoryStack = kashObject.directoryStack
}