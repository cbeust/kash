package com.beust.kash.api

/**
 * The result of a Kash command.
 */
class CommandResult(val returnCode: Int, val stdout: String? = null, val stderr: String? = null) {
    fun display() {
        if (stdout != null) {
            println(stdout)
        }
        if (stderr != null) {
            println(stderr)
        }

    }
}
