package com.beust.kash

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

@Test
class ScriptTest {
    private val kotlinEngine = ScriptEngineManager().getEngineByExtension("kash.kts")

    fun dependsOn() {
        val result = kotlinEngine.evalScript("""
            @file:DependsOn("log4j:log4j:1.2.12")
            val log = org.apache.log4j.Logger.getRootLogger()
            println(log.name)
            """.trimIndent())
        assertThat(result).isEqualTo(listOf("root"))
    }
}

private fun ScriptEngine.evalScript(script: String): List<String> = captureOut {
    eval(script)
}

private fun captureOut(body: () -> Unit): List<String> {
    val outStream = ByteArrayOutputStream()
    val prevOut = System.out
    System.setOut(PrintStream(outStream))
    try {
        body()
    } finally {
        System.out.flush()
        System.setOut(prevOut)
    }
    return outStream.toString().split('\r', '\n').mapNotNull { it.trim().takeIf { it.isNotEmpty() } }
}