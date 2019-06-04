<p align="center">
<img src="https://github.com/cbeust/kosh/blob/master/pictures/kash-logo.png?raw=true" width="50%"/>
</p>

Kash is a shell written in and powered by Kotlin.

The philosophy of Kash is to provide a minimal layer of compatibility with existing shells and then allow the user to write scripts in Kotlin for anything that requires more logic. This document is split in two parts:

1. [Kash's shell compabitility layer](#kash-as-shell)
2. [Kash and Kotlin](#kash-and-kotlin)

# Running Kash

Because Kash is still Alpha, releases are not yet available. In order to run it, you will need a JDK version greater or equal to 9 and to clone the repository:

```
$ git clone git@github.com:cbeust/kash
$ cd kash
$ export JAVA_HOME=... # JDK >= 9
$ ./run    # Build and run Kash

```
# [Kash as a shell](#kash-as-shell)

Kash supports the following features:

- Redirections (`stdout`, `stdin`):

```
$ ls > a.txt
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

> Note: how to set environment variables is covered in the [Kotlin section](#Kash and Kotlin)

- Changing directories

```
$ cd ..
$ cd - # go back to the previous directory
```

- Wildcards

```
$ ls *kts
build.gradle.kts
```

- Tab completion

> Note: Tab completion is still a work in progress and only works for a few built-in and predefined functions at the time of this writing.

# [Kash and Kotlin](#kash-and-kotlin)

## Kotlin evaluation

When a line is entered, Kash parses it and if a shell or built-in command can be found, it will run that. If not, Kash will evaluate the line as a Kotlin expression:

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
kotlin/kosh [master]$ git checkout dev
kotlin/kosh [dev]$ cd ..
Cedric/kotlin$ 
```

### Predef.kts

All the functions supported by Kash are defined in the file `Predef.kts`, which is shipped inside of Kash. You can [browse this file yourself](https://github.com:/cbeust/kash/master/blob/src/main/kotlin/com/beust/kash/Predef.kts) to get an idea of what other functionalities are available.


