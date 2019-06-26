package com.beust.kash

import com.beust.kash.parser.KashParser
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.StringReader

val idTransformer: TokenTransform = { word: Token.Word, s: List<String> -> s }

@Test
class Parser3Test {
    @DataProvider
    fun singleCommandDp() = arrayOf(
            arrayOf("ls", KashParser.SimpleCommand(listOf("ls"), null, null)),
            arrayOf("ls -l", KashParser.SimpleCommand(listOf("ls", "-l"), null, null)),
            arrayOf("ls -l > a.txt", KashParser.SimpleCommand(listOf("ls", "-l"), null, "a.txt")),
            arrayOf("ls -l < b.txt", KashParser.SimpleCommand(listOf("ls", "-l"), "b.txt", null)),
            arrayOf("ls -l < b.txt > a.txt", KashParser.SimpleCommand(listOf("ls", "-l"), "b.txt", "a.txt"))
    )

    @Test(dataProvider = "singleCommandDp")
    fun singleCommand(line: String, command: KashParser.SimpleCommand) {
        val sc = KashParser(StringReader(line))
        val goal = sc.SimpleCommand()
        println(goal)
        assertThat(goal.words).isEqualTo(command.words);
        assertThat(goal.input).isEqualTo(command.input);
        assertThat(goal.output).isEqualTo(command.output);
    }

    @DataProvider
    fun pipeCommandDp() = arrayOf(
        arrayOf("( ls )", KashParser.PipeCommand(listOf(listOf("ls")))),
        arrayOf("( ls | wc)", KashParser.PipeCommand(listOf(listOf("ls"), listOf("wc"))))
    )

    @Test(dataProvider = "pipeCommandDp")
    fun pipeCommand(line: String, command: KashParser.PipeCommand) {
        val sc = KashParser(StringReader(line))
        val goal = sc.SubShell()
        println(goal)
//        assertThat(goal.words).isEqualTo(command.words);
//        assertThat(goal.input).isEqualTo(command.input);
//        assertThat(goal.output).isEqualTo(command.output);
    }

    @DataProvider
    fun multiCommandDp() = arrayOf(
            arrayOf("ls -l | wc -l", KashParser.PipeCommand(listOf(listOf("ls", "-l"), listOf("wc", "-l")))),
            arrayOf("ls -l && echo a", KashParser.PipeCommand(listOf(listOf("ls", "-l"), listOf("echo", "a"))))
    )

    @Test(dataProvider = "multiCommandDp")
    fun multiCommand(line: String, command: KashParser.Command<List<List<String>>>) {
        val sc = KashParser(StringReader(line))
        val goal = sc.Goal2()
        assertThat(goal.content).isEqualTo(command.content)
    }
}

@Test
class ParserTest {
    private val parser = Parser(idTransformer)
    private fun word(n: String) = Token.Word(StringBuilder(n))
    private fun pipe() = Token.Pipe()
    private fun andAnd() = Token.AndAnd()
    private fun and() = Token.And()

    @DataProvider
    fun singleCommandDp() = arrayOf(
            arrayOf("ls -l", KashParser.SingleCommand(listOf("ls", "-l"), null, null))
    )

    @Test(dataProvider = "singleCommandDp")
    fun singleCommand(line: String, command: KashParser.Command<List<String>>) {
        val sc = KashParser(StringReader(line))
        val goal = sc.Goal2()
        assertThat(goal.content).isEqualTo(command.content)

    }

    @DataProvider
    fun multiCommandDp() = arrayOf(
            arrayOf("ls -l | wc -l", KashParser.PipeCommand(listOf(listOf("ls", "-l"), listOf("wc", "-l")))),
            arrayOf("ls -l && echo a", KashParser.PipeCommand(listOf(listOf("ls", "-l"), listOf("echo", "a"))))
    )

    @Test(dataProvider = "multiCommandDp")
    fun multiCommand(line: String, command: KashParser.Command<List<List<String>>>) {
        val sc = KashParser(StringReader(line))
        val goal = sc.Goal2()
        assertThat(goal.content).isEqualTo(command.content)
    }

    @DataProvider
    fun lexicalDp2() = arrayOf(
            arrayOf("ls -l", listOf("ls", "-l"), false),
            arrayOf("ls -l &", listOf("ls", "-l"), true)
    )

    @Test(dataProvider = "lexicalDp2")
    fun lexical2(line: String, words: List<String>, background: Boolean){
        val result = Parser2(idTransformer).parse(line)
        assertThat(result?.words).isEqualTo(words)
        assertThat(result?.background).isEqualTo(background)
    }

    @DataProvider
    fun lexicalDp() = arrayOf(
        arrayOf("test()", listOf(word("test"), Token.LeftParenthesis(), Token.RightParenthesis())),
        arrayOf("a 2>foo", listOf(word("a"), Token.TwoGreater(), word("foo"))),
        arrayOf("a2a", listOf(word("a2a"))),
        arrayOf("( sleep)", listOf(Token.LeftParenthesis(), word("sleep"), Token.RightParenthesis())),
        arrayOf("./gradlew", listOf(word("./gradlew"))),
        arrayOf("cd ~", listOf(word("cd"), word("~"))),
        arrayOf("ls|wc", listOf(word("ls"), pipe(), word("wc"))),
        arrayOf("ls 'a b c'", listOf(word("ls"), word("a b c"))),
        arrayOf("ls", listOf(word("ls"))),
        arrayOf("ls -l a b | wc -l",
            listOf(word("ls"), word("-l"), word("a"), word("b"), pipe(), word("wc"), word("-l"))),
        arrayOf("ls -l && wc -l", listOf(word("ls"), word("-l"), andAnd(), word("wc"), word("-l"))),
        arrayOf("ls -l&", listOf(word("ls"), word("-l"), and())),
        arrayOf("ls && wc", listOf(word("ls"), andAnd(), word("wc")))
    )

    @Test(dataProvider = "lexicalDp")
    fun lexical(command: String, expected:List<Token>) {
        assertThat(parser.lexicalParse(command)).isEqualTo(expected)
    }

    @DataProvider
    fun surroundedDp() = arrayOf(
        arrayOf("'a b \$c'", listOf(word("a b \$c")), "'"),
        arrayOf("\"a b \$c\"", listOf(word("a b \$c")), "\""),
        arrayOf("`a b \$c`", listOf(word("a b \$c")), "`")
    )

    @Test(dataProvider = "surroundedDp")
    fun surrounded(command: String, expected: List<Token>, surroundedBy: String) {
        val result = parser.lexicalParse(command)
        assertThat(result).isEqualTo(expected)
        assertThat((result[0] as Token.Word).surroundedBy).isEqualTo(surroundedBy)
    }

    fun exec(words: List<String>, input: String? = null, output: String? = null, error: String? = null): Exec {
        val w: ArrayList<Token>
                = ArrayList(words.map { Token.Word(StringBuilder(it), null) })
        if (output != null) {
            w.add(Token.Greater())
            w.add(Token.Word(StringBuilder(output)))
        }
        if (input != null) {
            w.add(Token.Less())
            w.add(Token.Word(StringBuilder(input)))
        }
        return Exec(w, input, output, error, idTransformer)
    }

    @DataProvider
    fun commandsDp(): Array<Array<out Any>> {
        return arrayOf(
            arrayOf("ls -l < a.txt",
                    listOf(Command.SingleCommand(exec(listOf("ls", "-l"), "a.txt", null)))),
            arrayOf("ls -l > b.txt",
                listOf(Command.SingleCommand(exec(listOf("ls", "-l"), null, "b.txt")))),
            arrayOf("ls -l |wc | something",
                listOf(Command.PipeCommands(
                    listOf(exec(listOf("ls", "-l")), exec(listOf("wc")),
                            exec(listOf("something")))))),
            arrayOf("ls -l && wc &&something",
                    listOf(Command.AndCommands(
                            listOf(exec(listOf("ls", "-l")), exec(listOf("wc")),
                                    exec(listOf("something"))))))

        )
    }

    @Test(dataProvider = "commandsDp")
    fun commands(line: String, expected: List<Command>) {
        assertThat(Parser(idTransformer).parse(line)).isEqualTo(expected)
    }
}