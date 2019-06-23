
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript {
    val kotlinVer by extra { "1.3.40-eap-105" }

    repositories {
        jcenter()
        mavenCentral()
        maven { setUrl("https://plugins.gradle.org/m2") }
    }

    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVer))
        classpath("com.github.jengelman.gradle.plugins:shadow:4.0.4")
    }
}

repositories {
    jcenter()
    mavenCentral()
    maven { setUrl("https://plugins.gradle.org/m2") }
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
}

plugins {
    java
    application
    idea
    id("org.jetbrains.kotlin.jvm") version "1.3.40-eap-105"
    id("com.github.johnrengelman.shadow") version "4.0.2"
    id("ca.coglinc.javacc") version "2.4.0"
}

val kotlinVer by extra { "1.3.40-eap-105" }

sourceSets {
    main {
        java.srcDir("build/generated/javacc")
    }
}

dependencies {
    listOf("org.jline:jline:3.11.0",
            "org.fusesource:fuse-project:7.2.0.redhat-060",
            "org.slf4j:slf4j-api:1.8.0-beta4",
            "ch.qos.logback:logback-classic:1.3.0-alpha4",
            "com.google.inject:guice:4.2.2",
            "me.sargunvohra.lib:CakeParse:1.0.7",
            "org.apache.ivy:ivy:2.4.0")
        .forEach { compile(it) }

    compile("com.beust:klaxon:5.0.5") {
        exclude("org.jetbrains.kotlin")
    }

    listOf("compiler-embeddable", "scripting-compiler-embeddable", "scripting-common", "scripting-jvm",
                "scripting-jvm-host-embeddable", "main-kts")
        .forEach { compile(kotlin(it, kotlinVer)) }

    listOf("org.testng:testng:6.13.1",
            "org.assertj:assertj-core:3.5.2")
        .forEach { testCompile(it) }
}

application {
    mainClassName = "com.beust.kash.MainKt"
}

val shadowJar = tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("kash-fat")
        mergeServiceFiles()
    }
}

// Disable standard jar task to avoid building non-shadow jars
val jar by tasks.getting {
    enabled = false
}
