/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.beust.kash.script

import org.jetbrains.kotlin.cli.common.repl.KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY
import org.jetbrains.kotlin.cli.common.repl.KOTLIN_SCRIPT_STATE_BINDINGS_KEY
import org.jetbrains.kotlin.mainKts.impl.FilesAndIvyResolver
import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.Repository
import java.io.File
import javax.script.Bindings
import javax.script.ScriptEngine
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.compat.mapLegacyDiagnosticSeverity
import kotlin.script.experimental.jvm.compat.mapLegacyScriptPosition
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.templates.standard.ScriptTemplateWithBindings

@Suppress("unused")
@KotlinScript(compilationConfiguration = CompilationConfiguration::class)
abstract class ScriptDefinition(val jsr223Bindings: Bindings) : ScriptTemplateWithBindings(jsr223Bindings) {

    private val myEngine: ScriptEngine? get() = bindings[KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY]?.let { it as? ScriptEngine }

    private inline fun <T> withMyEngine(body: (ScriptEngine) -> T): T =
        myEngine?.let(body) ?: throw IllegalStateException("Script engine for `eval` call is not found")

    fun eval(script: String, newBindings: Bindings): Any? =
        withMyEngine {
            val savedState =
                newBindings[KOTLIN_SCRIPT_STATE_BINDINGS_KEY]?.takeIf { it === this.jsr223Bindings[KOTLIN_SCRIPT_STATE_BINDINGS_KEY] }
                    ?.apply {
                        newBindings[KOTLIN_SCRIPT_STATE_BINDINGS_KEY] = null
                    }
            val res = it.eval(script, newBindings)
            savedState?.apply {
                newBindings[KOTLIN_SCRIPT_STATE_BINDINGS_KEY] = savedState
            }
            res
        }

    fun eval(script: String): Any? =
        withMyEngine {
            val savedState = jsr223Bindings.remove(KOTLIN_SCRIPT_STATE_BINDINGS_KEY)
            val res = it.eval(script, jsr223Bindings)
            savedState?.apply {
                jsr223Bindings[KOTLIN_SCRIPT_STATE_BINDINGS_KEY] = savedState
            }
            res
        }

    fun createBindings(): Bindings = withMyEngine { it.createBindings() }
}

object CompilationConfiguration : ScriptCompilationConfiguration(
    {
        defaultImports(DependsOn::class, Repository::class)
        refineConfiguration {
            onAnnotations(DependsOn::class, Repository::class, handler = ConfigurationRefiner())
        }
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
    })

class ConfigurationRefiner : RefineScriptCompilationConfigurationHandler {
    private val resolver = FilesAndIvyResolver()

    override operator fun invoke(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val diagnostics = arrayListOf<ScriptDiagnostic>()

        fun report(severity: ScriptDependenciesResolver.ReportSeverity, message: String, position: ScriptContents.Position?) {
            diagnostics.add(
                    ScriptDiagnostic(
                            message,
                            mapLegacyDiagnosticSeverity(severity),
                            context.script.locationId,
                            mapLegacyScriptPosition(position)
                    )
            )
        }

        val annotations = context.collectedData?.get(ScriptCollectedData.foundAnnotations)?.takeIf { it.isNotEmpty() }
                ?: return context.compilationConfiguration.asSuccess()

        val resolvedClassPath = try {
            val scriptContents = object : ScriptContents {
                override val annotations: Iterable<Annotation> = annotations.filter { it is DependsOn || it is Repository }
                override val file: File? = null
                override val text: CharSequence? = null
            }
            resolver.resolve(scriptContents, emptyMap(), ::report, null).get()?.classpath?.toList()
            // TODO: add diagnostics
        } catch (e: Throwable) {
            return ResultWithDiagnostics.Failure(*diagnostics.toTypedArray(), e.asDiagnostics(path = context.script.locationId))
        }

        return ScriptCompilationConfiguration(context.compilationConfiguration) {
            if (resolvedClassPath != null) updateClasspath(resolvedClassPath)
        }.asSuccess(diagnostics)
    }
}

