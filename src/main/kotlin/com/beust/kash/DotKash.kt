package com.beust.kash

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import java.io.File

/**
 * Read ~/.kash.json
 */
object DotKashJsonReader {
    private val DOT_KASH = File(System.getProperty("user.home"), ".kash.json")
    var dotKash: DotKash? = null

    init {
        if (DOT_KASH.exists()) {
            dotKash = Klaxon().parse(DOT_KASH)
        }
    }
}

class DotKash(
    @Json(name = "classPaths") val _classPath: List<String> = listOf(),
    @Json(name = "scriptPaths") val _scriptPath: List<String> = listOf(),
    @Json(name = "completers") val _completers: List<String> = listOf(),
    @Json(name = "extensions") val extensions: List<String>
) {
    val scriptPath: List<String> get() = _scriptPath.map { Tilde.expand(it) }
    val classPath: List<String> get() = _classPath.map { Tilde.expand(it) }
    val completers: List<String> get() = _completers.map { Tilde.expand(it) }
}
