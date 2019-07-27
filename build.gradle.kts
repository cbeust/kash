
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val kashVersion = "0.9"
val kashJarBase = "kash-$kashVersion"

allprojects {
    version = kashVersion
}

buildscript {
    val kotlinVer by extra { "1.3.41" }

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
    id("com.github.breadmoirai.github-release") version "2.2.9"
}

val kotlinVer by extra { "1.3.41" }

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
        archiveBaseName.set(kashJarBase)
        mergeServiceFiles()
    }
}

// Disable standard jar task to avoid building non-shadow jars
val jar by tasks.getting {
    enabled = false
}

//
// Release stuff. To create and upload the distribution to Github releases:
// ./gradlew dist
//

tasks.register("createScript") {
    dependsOn("assemble")
    File("$buildDir/release").apply {
        mkdirs()
        File(this, "kash").apply {
            writeText("java -jar $kashJarBase.jar")
            setExecutable(true)
        }
    }
}

tasks.register<Copy>("copyKash") {
    dependsOn("assemble")
    from("$buildDir/libs")
    into("$buildDir/release")
    include("*.jar")
}

// Defined in ~/.gradle/gradle.properties
val githubToken: String by project

githubRelease {
    token(githubToken)
    owner("cbeust")
    repo("kash")
    overwrite(true)
    releaseAssets("$buildDir/dist/kash-$kashVersion.zip")

// tagName("some tag")
// e.g. release notes
//    body("This is the body")
}

tasks.register<Zip>("dist") {
    // Create the script and copy the files to build/dist
    dependsOn("createScript")
    dependsOn("copyKash")

    // Create zip file from build/dist
    archiveFileName.set("kash-$kashVersion.zip")
    File("$buildDir/dist").apply {
        mkdirs()
        destinationDirectory.set(this)
    }

    from("$buildDir/release") {
        include("*")
    }

    // Upload to github releases
    dependsOn("githubRelease")
}
