package com.beust.kash

import com.beust.kash.api.IKashContext
import com.beust.kash.parser.SimpleList

interface ICommandFinder {
    /**
     * @return null if this finder is unable to parse the line. If the finder is able to parse the line
     * and is able to find a command, the return CommandSearchResult `lambda` field should contain a
     * lambda () -> CommandResult that will execute the found command. If the `lambda` field is `null`, then
     * no command was found.
     */
    fun findCommand(word: String, list: SimpleList?, context: IKashContext): CommandFinder.CommandSearchResult?
}