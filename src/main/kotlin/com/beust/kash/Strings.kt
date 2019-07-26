package com.beust.kash

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