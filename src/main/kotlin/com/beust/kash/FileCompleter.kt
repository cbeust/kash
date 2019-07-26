package com.beust.kash

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import java.io.File
import java.util.*

class FileCompleter(private val directoryStack: Stack<String>) : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        val word = line.words().last().replace("\\", "/")
        val last = word.lastIndexOf("/")
        val (dirAtCursor, patternAtCursor) = Strings.dirAndFile(word, directoryStack.peek())
        val dirAtCursorFile = File(Tilde.expand(dirAtCursor!!))
        val (dir, accept) =
            if (dirAtCursorFile.exists() && dirAtCursorFile.isDirectory) {
                Pair(dirAtCursorFile, { s: String -> s.startsWith(patternAtCursor) })
            } else {
                Pair(File(directoryStack.peek()), { s: String -> s.startsWith(word) })
            }

        dir.list().filter {
            accept(it)
        }.forEach {
            val cs = if (last != -1) "$dirAtCursor/$it" else it
            val cand = if (File(cs).isDirectory) "$cs/" else cs
            candidates.add(Candidate(cand))
        }
    }
}
