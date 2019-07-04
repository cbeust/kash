package com.beust.kash

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.util.*

@Test
class TokenTransformerTest {
    @DataProvider
    fun envTransformerTestDp() = arrayOf(
            arrayOf("a\$var1 cdef\$var2", "ab cdefg"),
            arrayOf("a\${var1}cdef\${var2}", "abcdefg"),
            arrayOf("a\$var1 cdef\${var2}", "ab cdefg")
    )

    @Test(dataProvider = "envTransformerTestDp")
    fun envTransformerTest(s: String, expected: String) {
        val env = mapOf("var1" to "b", "var2" to "g")
        val result = EnvVariableTransformer(env).transform(listOf(s))
        assertThat(result).isEqualTo(listOf(expected))
    }

    @Test(enabled = false, description = "Need a correct directory")
    fun globTransformerTest() {
        val directoryStack = Stack<String>().apply { push("/users/cedricbeust/kotlin/kosh") }
        val t = GlobTransformer(directoryStack).transform(listOf("*kts"))
        assertThat(t).isEqualTo(listOf("a.kts", "build.gradle.kts"))
    }


}