package com.beust.kash

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

val idTransformer: TokenTransform = { word: Token.Word, s: List<String> -> s }

@Test
class ParserTest {
    private val parser = Parser(idTransformer)
    private fun word(n: String) = Token.Word(StringBuilder(n))
    private fun pipe() = Token.Pipe()
    private fun andAnd() = Token.AndAnd()
    private fun and() = Token.And()

    @DataProvider
    fun lexicalDp() = arrayOf(
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

    fun exec(words: List<String>, input: String? = null, output: String? = null): Exec {
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
        return Exec(w, input, output, idTransformer)
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