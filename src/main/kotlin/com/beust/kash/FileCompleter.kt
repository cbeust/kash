package com.beust.kash

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

class FileCompleter(private val directoryStack: Stack<String>) : Completer {
    private val log = LoggerFactory.getLogger(FileCompleter::class.java)

    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        val word = line.words().last()
        val dirAtCursor = File(Tilde.expand(line.word()))
        val (dir, accept) = if (dirAtCursor.exists() && dirAtCursor.isDirectory) {
            Pair(dirAtCursor, { s: String -> true })
        } else {
            Pair(File(directoryStack.peek()), { s: String -> s.startsWith(word) })
        }

        log.debug("Completing at dir $dir")

        dir.list().filter {
            accept(word)
        }.forEach {
            candidates.add(Candidate(it + (if (File(it).isDirectory) "/" else "")))
        }
    }
}
