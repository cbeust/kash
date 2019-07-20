/**
 * Demonstrate a Kash script depending on an external Maven dependency.
 */

@file:org.jetbrains.kotlin.script.util.DependsOn("org.kohsuke:github-api:1.95")

import org.kohsuke.github.GitHub

fun github(name: String = "cbeust/kash") {
    val github = GitHub.connectAnonymously()
    val repo = github.getRepository(name)
    println("Repo: " + repo.name + ": " + repo.description)
}

if (args.isEmpty()) {
    github()
} else {
    github(args[0])
}
