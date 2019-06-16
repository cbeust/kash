package com.beust.kash

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import java.io.File

/**
 * Read ~/.kash.json
 */
object DotKashReader {
    private val DOT_KASH = File(System.getProperty("user.home"), ".kash.json")
    var dotKash: DotKash? = null

    init {
        if (DOT_KASH.exists()) {
            dotKash = Klaxon().parse(DOT_KASH)
        }
    }
}

class DotKash(
        @Json(name = "classPath") val _classPath: List<String>,
        @Json(name = "scriptPath") val _scriptPath: List<String>) {
    val scriptPath: List<String>
        get() = _scriptPath.map { Tilde.expand(it) }
    val classPath: List<String>
        get() = _classPath.map { Tilde.expand(it) }
}
