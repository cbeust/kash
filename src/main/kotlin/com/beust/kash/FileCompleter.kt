package com.beust.kash

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import java.io.File
import java.util.*

class FileCompleter(private val directoryStack: Stack<String>) : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        val word = line.words().last()
        File(directoryStack.peek()).list().filter {
            it.startsWith(word)
        }.forEach {
            candidates.add(Candidate(it + (if (File(it).isDirectory) "/" else "")))
        }
    }
}