<p align="center">
<img src="https://github.com/cbeust/kash/blob/master/pictures/logo-kash.png?raw=true" width="50%"/>
</p>

Kash is a shell written in and powered by Kotlin.

The philosophy of Kash is to provide a minimal layer of compatibility with existing shells and then allow the user to write scripts in Kotlin for anything that requires more logic.

This document is split in two parts:

1. [Kash's shell compabitility layer](#kash-as-a-shell)
2. [Kash and Kotlin](#kash-and-kotlin)

# Running Kash

- Kash requires Java 1.11 or higher (Java 1.8 will soon be added).
- Download the [latest release of Kash](https://github.com/cbeust/kash/releases/latest).
- Unzip the zip file into your `bin` directory.
- Run `kash`.

# Kash as a shell

Kash supports the following features:

- Redirections (`stdout`, `stdin`, `stderr`):

```
$ ls > a.txt 2>error.txt
$ cat < a.txt
```

- Pipes:

```
$ ls | wc
11      11     86       
```

- Environment variables

```
$ echo $PATH
/usr/bin:...
```

> Note: how to set environment variables is covered in the [Kotlin section](#kash-and-kotlin)

- Changing directories

```
$ cd ..
$ cd -    # go back to the previous directory
$ cd ~    # go home
```

- Wildcards

```
$ ls *kts
build.gradle.kts
```

- Tab completion

```
$ ls kash<TAB>
kash         kash-debug
```

# Kash and Kotlin

## Kotlin evaluation

When a line is entered and Kash determines it's not a shell command, Kash will evaluate the line as a Kotlin expression:

```
$ fun h() = "hello"
$ h()
hello
```

Shell and Kotlin can be embedded within each other with the ` (backtick) character. You can either embed Kotlin in a shell command or embed a shell command within a Kotlin expression:

```
$ val a="README.md"
README.md
$ ls `a`
README.md
```

## Predefined functions

Kash favors calling Kotlin functions for as many operations as possible.

### Path

```
$ path()
/bin
/usr/bin
$ path("/usr/local/bin")
$ path()
/bin
/usr/bin
/usr/local/bin 
```

### Current directory

```
$ pwd()
~/kotlin/kash
```

### Environment

The environment is controlled by the function and value `kenv` (to avoid colliding with `env`, which is likely
to be on your path):

```
$ kenv("foo", "bar")
$ kenv("foo")
bar
$ echo $foo   # Note that you can access environment variables the shell way too
bar
$ kenv   # display the whole environment
a=b
foo=bar
```

### Prompt

The prompt is changed with the `prompt()` function. You can pass either a simple string to that function, or a Kotlin expression surrounded by `, in which case, that expression will be evaluated each time:

```
$ prompt("test$ ")
test$ prompt("`java.util.Date().toString() + '$'`")
Mon Jun 03 21:38:55 PDT 2019$      # <press RETURN> 
Mon Jun 03 21:39:29 PDT 2019$
```

See the [.kash.kts](#.kash.kts) section for a more complex prompt implementation.

### os()

The `os()` function is defined as follows:

```kotlin
fun os(line: String): CommandResult?
```

It lets you launch an operating system command and capture its output in [`CommandResult`](https://github.com/cbeust/kash/blob/master/src/main/kotlin/com/beust/kash/CommandResult.kt) instance, which is returned. This allows you to process the output of executables in Kotlin:

```
$ os("ls")?.stdout?.toUpperCase()
BUILD
BUILD.GRADLE.KTS
GRADLE
GRADLEW
```

### .kash.kts

A file called `~/.kash.kts` in the user's home directory will automatically be read by Kash when starting, allowing users to define their own functions and environment. For example, here is how to create a prompt that contains the current directory in short form followed by the current git branch, if in a Git directory:

```
// ~/.kash.kts

/**
 * Display the current git branch if in a git directory or an empty string otherwise
 */
fun gitBranch() = File(pwd(), ".git/HEAD").let { head ->
    if (head.exists()) {
        val segments = head.readLines()[0].split("/")
        " [" + segments[segments.size - 1] + "]"
    } else {
        ""
    }
}

/**
 * Display the last two segments of the current directory
 */
fun currentDir() = Paths.get(pwd()).let { path ->
    path.nameCount.let { size ->
        if (size > 2) path.getName(size - 2).toString() + "/" + path.getName(size - 1).toString()
        else path.toString()
    }
}

fun myPrompt() = currentDir() + gitBranch() + "\u001B[32m$ "

prompt("`myPrompt()`")
```

The prompt now looks like this:

```
kotlin/kash [master]$ git checkout dev
kotlin/kash [dev]$ cd ..
Cedric/kotlin$ 
```

### Predef.kts

All the functions supported by Kash are defined in the file `Predef.kts`, which is shipped inside of Kash. You can [browse this file yourself](https://github.com/cbeust/kash/blob/master/src/main/resources/kts/Predef.kts) to get an idea of what other functionalities are available.

# Programming Kash

Kash offers a variety of functionalities to let you program it in Kotlin.

## Kash configuration files

Kash can be configured with two different files: `~/.kash.json`  and `~/.kash.kts`.

### ~/.kash.json

This file is a JSON file that lets you configure Kash with a few parameters:

- `classPaths`:  An array of strings pointing to the classpath that Kash will be started with.
- `scriptPaths`: An array of strings pointing to directories where scripts are located.

Here is a sample `~/.kash.json`:

```json
{
    "classPaths": [
        "~/build/classes"
    ],
    "scriptPaths": [
        "~/kash-scripts"
    ]
}
```

### ~/.kash.kts

This file contains valid Kotlin code and will be run at start up, allowing you to define functions and variables
that you need.

## Writing scripts with Kash

Kash script files are regular Kotlin Script files but with a few [additions](#kash-additions). We recommend using the suffix
`.kash.kts` for your Kash files, which will provide additional support in IDEA for these files.

Here is an example Kash script named `a.kash.kts`:

```kotlin
// a.kash.kts
fun hi(s: String = "Unknown") = println("Hello, $s")

if (args.size > 0) hi(args[0]) else hi()
```

Assuming you have defined your `~/.kash.json` as shown above, save this file as `~/.kash-scripts/a.kash.kts`. All you
need to do now is just type `a` in Kash and this code will be executed:

```
$ a
Hello, Unknown
```
Since this script file contains some code to parse the `args` parameter, you can actually pass it parameters:

```
$ a Cedric
Hello, Cedric
```

This logic is executed by the following code from the script file:

```
if (args.size > 0) hi(args[0]) else hi()
```

Of course, as soon as you load the script, the function `hi()` gets defined, so you can still invoke it directly
if you prefer:

```
$ hi()
Hello, Unknown
$ hi("Cedric")
Hello, Cedric
$
```

## Kash additions

In addition to regular `.kts` files, Kash has a few additional functionalities.

### Annotation `@file:DependsOn`

This annotation allows you to tell Kash that your script depends on extra libraries which are defined by Maven coordinates. Here is a small example that automatically loads the `log4j` library:

```kotlin
// log.kash.kts
@file:DependsOn("log4j:log4j:1.2.12")
val log = org.apache.log4j.Logger.getRootLogger()
println(log.name)
```

One way to invoke this file:
```
$ . log.kash.kts
root
$
```

Of course, you can also put this file on your `scriptPaths` and then just invoke it with `log`.

Here is another example of a script witha Maven dependency that uses a Github library:

```kotlin
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
```

Put this file in `~/kash-scripts/github.kash.kts` and run it:

```
$ github
kash: A shell powered by Kotlin
$ github cbeust/klaxon
klaxon: A JSON parser for Kotlin
```

# Community

Join the [Kash Slack channel](https://kotlinlang.slack.com/messages/kash_shell).
