package com.beust.kash.api

@Retention(AnnotationRetention.RUNTIME)
annotation class Builtin(val value: String = "")

@Retention(AnnotationRetention.RUNTIME)
annotation class TabCompleter
