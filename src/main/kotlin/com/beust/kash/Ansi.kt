@file:Suppress("MayBeConstant", "MemberVisibilityCanBePrivate", "unused")

package com.beust.kash

object Ansi {
    val RESET = "\u001B[0m"

    val BLACK = "\u001B[30m"
    val RED = "\u001B[31m"
    val GREEN = "\u001B[32m"
    val YELLOW = "\u001B[33m"
    val BLUE = "\u001B[34m"
    val PURPLE = "\u001B[35m"
    val CYAN = "\u001B[36m"
    val WHITE = "\u001B[37m"

    val BRIGHT_BLACK = "\u001B[90m"
    val BRIGHT_RED = "\u001B[91m"
    val BRIGHT_GREEN = "\u001B[92m"
    val BRIGHT_YELLOW = "\u001B[93m"
    val BRIGHT_BLUE = "\u001B[94m"
    val BRIGHT_PURPLE = "\u001B[95m"
    val BRIGHT_CYAN = "\u001B[96m"
    val BRIGHT_WHITE = "\u001B[97m"

    val FOREGROUNDS = arrayOf(BLACK, RED, GREEN, YELLOW, BLUE, PURPLE, CYAN, WHITE, BRIGHT_BLACK, BRIGHT_RED,
            BRIGHT_GREEN, BRIGHT_YELLOW, BRIGHT_BLUE, BRIGHT_PURPLE, BRIGHT_CYAN, BRIGHT_WHITE)

    val BG_BLACK = "\u001B[40m"
    val BG_RED = "\u001B[41m"
    val BG_GREEN = "\u001B[42m"
    val BG_YELLOW = "\u001B[43m"
    val BG_BLUE = "\u001B[44m"
    val BG_PURPLE = "\u001B[45m"
    val BG_CYAN = "\u001B[46m"
    val BG_WHITE = "\u001B[47m"

    val BRIGHT_BG_BLACK = "\u001B[100m"
    val BRIGHT_BG_RED = "\u001B[101m"
    val BRIGHT_BG_GREEN = "\u001B[102m"
    val BRIGHT_BG_YELLOW = "\u001B[103m"
    val BRIGHT_BG_BLUE = "\u001B[104m"
    val BRIGHT_BG_PURPLE = "\u001B[105m"
    val BRIGHT_BG_CYAN = "\u001B[106m"
    val BRIGHT_BG_WHITE = "\u001B[107m"

    val BACKGROUNDS = arrayOf(BG_BLACK, BG_RED, BG_GREEN, BG_YELLOW, BG_BLUE, BG_PURPLE, BG_CYAN, BG_WHITE,
            BRIGHT_BG_BLACK, BRIGHT_BG_RED, BRIGHT_BG_GREEN, BRIGHT_BG_YELLOW, BRIGHT_BG_BLUE, BRIGHT_BG_PURPLE,
            BRIGHT_BG_CYAN, BRIGHT_BG_WHITE)
}