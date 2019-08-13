package com.beust.kash.api

import java.util.*

interface IKashContext {
    val directoryStack: Stack<String>
    val env: HashMap<String, String>
    val paths: ArrayList<String>
    val scriptPaths: List<String>
    val prompt: String
    val lineRunner: ILineRunner
}