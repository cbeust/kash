package com.beust.kash

import com.beust.klaxon.Klaxon
import java.io.File

/**
 * Read ~/.kash.json
 */
class DotKashReader {
    private val DOT_KOSH = File(System.getProperty("user.home"), ".kash.json")
    var dotKash: DotKosh? = null

    init {
        if (DOT_KOSH.exists()) {
            dotKash = Klaxon().parse(DOT_KOSH)
        }
    }
}

class DotKosh(val classpath: List<String>)
