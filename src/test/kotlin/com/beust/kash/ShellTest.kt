package com.beust.kash

import com.google.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

@Test
@org.testng.annotations.Guice(modules = [KashModule::class])
class ShellTest @Inject constructor(private val shell: Shell, private val context: IKashContext){
    private val CommandResult.out get() = this.stdout?.replace("\r", "")

    private fun runLine(line: String) = shell.runLine(line, context, false)

    @DataProvider
    fun simpleDp() = arrayOf(
            arrayOf("echo src", "src\n"),
            arrayOf("ls `echo src`", "main\ntest\n")
    )

    @Test(dataProvider = "simpleDp")
    fun simple(line: String, expected: String) {
        val result = runLine(line)
        assertThat(result.out).isEqualTo(expected)
    }

    fun pipe() {
        val result = runLine("echo a\nb\nc | wc")
        assertThat(result.stdout).startsWith("      1       3       6")
    }

    fun env() {
        val result = runLine("echo \$A")
        assertThat(result.stdout).isNull()
        runLine("""kenv("A","B")""")
        val result2 = runLine("echo \$A")
        assertThat(result2.out).isEqualTo("B\n")
    }
}