package com.beust.kash

sealed class Token {
    val isWord = this is Word
    val isSeparator = this is Pipe || this is AndAnd || this is SemiColon

    class Word(val _name: StringBuilder, val surroundedBy: String? = null): Token() {
        override fun equals(other: Any?) = other is Word && other.name == name
        override fun toString(): String {
            val name2 =
                    if (surroundedBy == null) name
                    else surroundedBy + name + surroundedBy
            return "Word(\"$name2\")"
        }

        /** The actual name of this token after transformation */
        private var _hidden: List<String>? = null
        var name: List<String>
            get() = if (_hidden == null) listOf(_name.toString()) else _hidden!!
            set(v) { _hidden = v }
    }

    data class Pipe(val c: Char = '|'): Token()
    data class And(val c: Char = '&'): Token()
    data class AndAnd(val c: String = "&&"): Token()
    data class Greater(val c: Char = '>'): Token()
    data class GreaterGreater(val c: String = ">>"): Token()
    data class Less(val c: Char = '<'): Token()
    data class SemiColon(val c: Char = ';'): Token()
    data class LeftParenthesis(val c: Char = '('): Token()
    data class RightParenthesis(val c: Char = ')'): Token()
}

