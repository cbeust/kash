package com.beust.kash

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object Streams {
    fun readStream(ins: InputStream): String? {
        val reader = BufferedReader(InputStreamReader(ins))

        val builder = StringBuilder()
        var line = reader.readLine()
        while (line != null) {
            builder.append(line)
            builder.append(System.getProperty("line.separator"))
            line = reader.readLine()
        }
        val result = builder.toString()
        return if (result.isBlank()) null else result
    }
}