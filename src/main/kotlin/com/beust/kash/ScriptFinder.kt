package com.beust.kash

import com.beust.kash.api.CommandResult
import com.beust.kash.api.IKashContext
import com.beust.kash.parser.SimpleList
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader

/**
 * Find Kash scripts from `scriptPaths`.
 */
class ScriptFinder @Inject constructor(private val engine: Engine): ICommandFinder {
    private val log = LoggerFactory.getLogger(ScriptFinder::class.java)

    override fun findCommand(word: String, list: SimpleList?, context: IKashContext): CommandFinder.CommandSearchResult? {
        val scriptPath = context.scriptPaths
        // See if this is a .kash.kts script
        scriptPath.forEach { path ->
            val c1 =
                    if (word.startsWith(".") || word.startsWith("/")) word
                    else path + File.separatorChar + word
            val file = File(
                    if (c1.endsWith("kash.kts")) c1
                    else "$c1.kash.kts"
            )
            if (file.exists() and file.isFile) {
                log.debug("Found script: " + file.absolutePath)
                val lambda = {
                    log.debug("Detected script")
                    val words = list!!.toWords()
                    val result = engine.eval(FileReader(File(file.absolutePath)),
                            words.subList(1, words.size))
                    CommandResult(0, result?.toString())
                }
                return CommandFinder.CommandSearchResult(file.absolutePath, lambda)
            }
        }

        return null
    }
}