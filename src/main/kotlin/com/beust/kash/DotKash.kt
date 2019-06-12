package com.beust.kash

import com.beust.klaxon.Klaxon
import java.io.File

/**
 * Read ~/.kash.json
 */
class DotKashReader {
    private val DOT_KASH = File(System.getProperty("user.home"), ".kash.json")
    var dotKash: DotKosh? = null

    init {
        if (DOT_KASH.exists()) {
            dotKash = Klaxon().parse(DOT_KASH)
        }
    }
}

class DotKosh(val classpath: List<String>)
