package com.beust.kash

import com.beust.kash.api.Builtin
import com.beust.kash.parser.SimpleList
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class LaunchableBuiltin(val instance: Any, val name: String, val lambda: KCallable<CommandResult>)

class BuiltinFinder(private val instances: List<Any>): ICommandFinder {

    /** Public so we can create a StringsCompleter from all the builtins */
    val builtinMap = findBuiltins(instances)

    private fun findBuiltins(instances: List<Any>): Map<String, LaunchableBuiltin> {
        val result = hashMapOf<String, LaunchableBuiltin>()
        instances.map { inst ->
            Pair(inst::class, inst)
        }.map {
            findBuiltIns(it.first, it.second)
        }.forEach {
            result.putAll(it)
        }
        return result
    }

    /**
     * Look up all the functions annotated with @Builtin on the given class.
     */
    private fun findBuiltIns(cls: KClass<*>, instance: Any): Map<String, LaunchableBuiltin> {
        val result = cls.members
                .map { it to it.annotations }
                .filter { it.second.isNotEmpty() }
                .map { Pair(it.first as KCallable<CommandResult>, it.second.first()) }
                .filter {
                    verifyBuiltinFunction(it.first, cls)
                    it.second is Builtin
                }.map {
                    val bi = it.second as Builtin
                    val name = if (bi.value == "") it.first.name else bi.value
                    Pair(name, LaunchableBuiltin(instance, name, it.first))
                }
                .toMap()
        return result
    }

    private fun verifyBuiltinFunction(function: KCallable<CommandResult>, cls: KClass<*>) {
        val rt = function.returnType.toString()
        if (rt != CommandResult::class.java.name) {
            throw IllegalArgumentException("Built-in function ${cls}.${function.name} should return a CommandResult, not $rt")
        }
        // TOOD: need to check the parameters too: (List<String>)
        val par1 = function.parameters[1]
        val jc = par1.javaClass
    }

    override fun findCommand(word: String, list: SimpleList?, context: IKashContext): CommandFinder.CommandSearchResult? {
        val launchableBuiltin = builtinMap[word]
        val result =
            if (launchableBuiltin != null) {
                val words = list!!.toWords()
                val bi = launchableBuiltin.lambda
                val callable = { bi.call(launchableBuiltin.instance, words) }
                CommandFinder.CommandSearchResult(word, callable)
            } else {
                null
            }
        return result
    }
}