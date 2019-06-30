package com.beust.kash

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
