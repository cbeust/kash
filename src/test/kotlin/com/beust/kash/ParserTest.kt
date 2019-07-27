package com.beust.kash

import com.beust.kash.parser.*
import com.beust.kash.word.KashWordParser
import com.beust.kash.word.WordFragment
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.StringReader

@Test
class Parser3Test {
    private fun words(vararg s: String) = listOf(SimpleCmd(s.toList()))
    private fun words(s: List<String>) = listOf(SimpleCmd(s))

    private fun word(s: String) = WordFragment(s, true)
    private fun env(s: String) = WordFragment(s, false)

    @DataProvider
    fun wordDp() = arrayOf(
            arrayOf("c:\\users", listOf(word("c:\\users"))),
            arrayOf("ab", listOf(word("ab"))),
            arrayOf("ab \$var1", listOf(word("ab"), word(" "), env("var1"))),
            arrayOf("ab \${var1}", listOf(word("ab"), word(" "), env("var1")))
        )

    @Test(dataProvider = "wordDp")
    fun wordTest(line: String, expected: List<WordFragment>) {
        val p = KashWordParser(StringReader(line))
        val fragments = p.ParsedWord()
        assertThat(fragments).isEqualTo(expected)
    }

    @DataProvider
    fun singleCommandDp() = arrayOf(
            arrayOf("ls", SimpleCommand(words("ls"), null, null)),
            arrayOf("ls -l", SimpleCommand(words("ls", "-l"), null, null)),
            arrayOf("ls -l > a.txt", SimpleCommand(words("ls", "-l"), null, "a.txt")),
            arrayOf("ls -l < b.txt", SimpleCommand(words("ls", "-l"), "b.txt", null)),
            arrayOf("ls -l < b.txt > a.txt", SimpleCommand(words("ls", "-l"), "b.txt", "a.txt"))
    )

    @Test(dataProvider = "singleCommandDp")
    fun singleCommand(line: String, expected: SimpleCommand) {
        val sc = KashParser(StringReader(line))
        val goal = sc.SimpleCommand()
        assertThat(goal.content).isEqualTo(expected.content);
        assertThat(goal.input).isEqualTo(expected.input);
        assertThat(goal.output).isEqualTo(expected.output);
    }

    @DataProvider
    fun subShellDp() = arrayOf(
        arrayOf("( ls )", SubShell(CompoundList(
                listOf(SimpleCommand(words("ls"), null, null)))))
//        arrayOf("( ls | wc)", SubShell(CompoundList(listOf(
//
//                PipeCommand(listOf(listOf("ls"), listOf("wc"))))))),
//        arrayOf("( ls -l > a.txt| wc)", PipeCommand(listOf(listOf("ls", "-l"), listOf("wc"))))
    )

    @Test(dataProvider = "subShellDp")
    fun subShell(line: String, expected: SubShell) {
        val sc = KashParser(StringReader(line))
        val goal = sc.SubShell()
        assertThat(goal.command.content).isEqualTo(expected.command.content);
    }

    private fun simpleCommand(vararg word: String, input: String? = null, output: String? = null)
            = SimpleCommand(words(word.toList()), input, output, null)

    @DataProvider
    fun commandDp(): Array<Array<Any>> {
        val sc = simpleCommand("ls", "-l")
        return arrayOf(
                arrayOf("ls -l > a.txt", true, simpleCommand("ls", "-l", output = "a.txt")),
                arrayOf("ls -l", true, sc),
                arrayOf("(ls -l)", false, SubShell(CompoundList(listOf(sc)))))
    }

    @Test(dataProvider = "commandDp")
    fun <T> command(line: String, isSimple: Boolean, expected: Any) {
        val sc = KashParser(StringReader(line))
        val goal = sc.Command()
        if (isSimple) {
            assertThat(goal.simpleCommand).isEqualTo(expected)
        } else {
            assertThat(goal.subShell).isEqualTo(expected)
        }
    }

    private fun command(args: List<String>) = Command(SimpleCommand(words(args), null, null, null), null)

    @DataProvider
    fun simpleListDp(): Array<Array<Any>> {
        val sc = simpleCommand("ls", "-l")
        return arrayOf(
                arrayOf("ls -l && echo", simpleCommand("ls", "-l"))
        )
    }

    @Test(dataProvider = "simpleListDp")
    fun simpleList(line: String, expected: Any) {
        val parser = KashParser(StringReader(line))
        val result = parser.SimpleList()
        assertThat(result.content[0]).isEqualTo(PipeLineCommand(listOf(command(listOf("ls", "-l"))), null))
        assertThat(result.content[1]).isEqualTo(PipeLineCommand(listOf(command(listOf("echo"))), "&&"))
    }
}
