package com.beust.kash

import com.beust.kash.parser.SimpleList

object Strings {
    /**
     * Separate a word such as "~/abc/def/j" into Pair("~/abc/def", "j").
     */
    fun dirAndFile(word: String, defaultDir: String? = null): Pair<String?, String> {
        val w = word.replace("\\", "/")
        val last = w.lastIndexOf("/")
        val result =
                if (last != -1) Pair(w.substring(0, last), word.substring(last + 1))
                else Pair(defaultDir, w)
        return result
    }
}

fun SimpleList.toWords(): List<String> {
    val pipelineCommand = this.content
    val simpleCommands = pipelineCommand.flatMap { it.content }
    val words = arrayListOf<String>()
    simpleCommands.forEach { w ->
        if (w.simpleCommand != null) {
            words.addAll(w.simpleCommand.words)
        } else {
            TODO("Need to handle subShell: " + w.subShell)
        }
    }
    return words
}
