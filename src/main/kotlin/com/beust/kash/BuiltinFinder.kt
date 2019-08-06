package com.beust.kash

import com.beust.kash.api.Builtin
import com.beust.kash.parser.SimpleList
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class LaunchableBuiltin(val instance: Any, val name: String, val lambda: KCallable<CommandResult>)

class BuiltinFinder(instances: List<Any>, classNames: List<String>): ICommandFinder {

    /** Public so we can create a StringsCompleter from all the builtins */
    val builtinMap = findBuiltins(instances, classNames)

    private fun findBuiltins(instances: List<Any>, classNames: List<String>): Map<String, LaunchableBuiltin> {
        val allInstances = instances + instantiateClasses(classNames)
        val result = hashMapOf<String, LaunchableBuiltin>()
        allInstances.map { instance ->
            findBuiltins(instance::class, instance)
        }.forEach {
            result.putAll(it)
        }
        return result
    }

    private fun instantiateClasses(classNames: List<String>) =
        classNames.map {
            val constructor = Class.forName(it).constructors.firstOrNull {
                it.parameterCount == 0
            }
            if (constructor != null) {
                constructor.newInstance()
            } else {
                throw java.lang.IllegalArgumentException("Couldn't instantiate $it")
            }
        }
    /**
     * Look up all the functions annotated with @Builtin on the given class.
     */
    private fun findBuiltins(cls: KClass<*>, instance: Any): Map<String, LaunchableBuiltin> {
        val result = cls.members
                .asSequence()
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
                .toList()
                .toMap()
        return result
    }

    private fun verifyBuiltinFunction(function: KCallable<CommandResult>, cls: KClass<*>) {
        fun error(s: String) {
            throw IllegalArgumentException("Built-in function \"${cls.simpleName}.${function.name}()\" $s")
        }

        val rt = function.returnType.toString()
        if (rt != CommandResult::class.java.name) {
            error("should return a CommandResult, not $rt")
        }
        if (function.parameters.size != 3) {
            error("should have exactly two parameters of types List<String> and IKashContext")
        }
        // TOOD: need to check the parameters too: (List<String>)
//        if (function.parameters[1].type.classifier != kotlin.collections.Collection<String>::class) {
//            error("'s parameter #2 should be of type List<String>")
//        }
        if (function.parameters[2].type.classifier != IKashContext::class) {
            error("'s parameter #2 should be of type IKashContext")
        }
    }

    override fun findCommand(word: String, list: SimpleList?, context: IKashContext): CommandFinder.CommandSearchResult? {
        val launchableBuiltin = builtinMap[word]
        val result =
            if (launchableBuiltin != null) {
                val words = list!!.toWords()
                val bi = launchableBuiltin.lambda
                val callable = { bi.call(launchableBuiltin.instance, words, context) }
                CommandFinder.CommandSearchResult(word, callable)
            } else {
                null
            }
        return result
    }
}