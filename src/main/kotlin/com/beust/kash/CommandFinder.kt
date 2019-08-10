package com.beust.kash

import com.beust.kash.api.CommandResult
import com.beust.kash.api.IKashContext
import com.beust.kash.parser.SimpleList

class CommandFinder(private val finders: List<ICommandFinder>): ICommandFinder {
    /**
     * Describe the result of search for a command. A command can be an executable, a script, or a built-in command.
     */
    class CommandSearchResult(val _path: String, val lambda: () -> CommandResult)

    override fun findCommand(word: String, list: SimpleList?, context: IKashContext): CommandSearchResult? {
        return finders.mapNotNull { it.findCommand(word, list, context) }.firstOrNull()
    }
}
