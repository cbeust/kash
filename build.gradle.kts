import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript {
    val kotlinVer by extra { "1.3.31" }

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
}

apply<ApplicationPlugin>()

application {
    mainClassName = "com.beust.kash.MainKt"
}

plugins {
    java
    application
    idea
    id("org.jetbrains.kotlin.jvm") version "1.3.31"
}

dependencies {
    listOf("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.3.31",
            "org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:1.3.31",
            "org.jetbrains.kotlin:kotlin-script-util:1.3.31",
            "org.jline:jline:3.11.0",
            "org.fusesource:fuse-project:7.2.0.redhat-060",
            "org.slf4j:slf4j-api:1.8.0-beta4",
            "org.slf4j:slf4j-simple:1.8.0-beta4",
            "com.beust:klaxon:5.0.5"
            )
        .forEach { compile(it) }

    listOf("org.testng:testng:6.13.1",
            "org.assertj:assertj-core:3.5.2")
        .forEach { testCompile(it) }
}

/////
// Shadow jar
//

apply {
    plugin("com.github.johnrengelman.shadow")
}

val shadowJar = tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("kash-fat")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "com.beust.kash.MainKt"))
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}
