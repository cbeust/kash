package com.beust.kash

import com.beust.kash.parser.KashParser
import java.io.StringReader

fun main() {
    listOf(
            "ls a"
            ,
            "ls b | wc -l"
            ,
            "ls c && echo a"
    ).forEach {
        val sc = KashParser(StringReader(it))
//        val goal = sc.Goal2()
//        println(goal)
    }
}

class Parser3 {

}