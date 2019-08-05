package com.beust.kash

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import java.io.FileReader

/**
 * External tab completers are defined in ~/.kash.json and are simple Kash scripts:
 *
 * {
 *     "completers": [
 *           "gitCompleter.kash.kts"
 *     ]
 * }
 * These scripts are looked up on the `scriptPaths` and they are passed the following arguments:
 * - args[0] (String): the full line typed so far
 * - args[1] (Int): the position of the cursor
 * The script needs to return a `List<String>` containing all the candidates, or an empty
 * list if no completion is possible at this point.
 */
@Suppress("UNCHECKED_CAST")
class ExternalCompleter(private val context: KashContext, val engine: Engine): Completer {
    override fun complete(reader: LineReader?, line: ParsedLine, candidates: MutableList<Candidate>) {
        val completers = DotKashJsonReader.dotKash?.completers
        val finder = ScriptFinder(engine)
        completers?.forEach {
            val result = finder.findCommand(it, null, context)
            if (result != null) {
                val cs = engine.eval(FileReader(result._path), listOf(line.line(), line.cursor().toString()))
                    as List<String>
                cs.forEach { candidate ->
                    val group = it.substring(0, it.length - ".kash.kts".length)
                    candidates.add(Candidate(candidate, candidate, group, null, null, null, true))
                }
            } else {
                System.err.println("\nWARNING: Couldn't find tab completer $it")
            }
        }
    }
}
