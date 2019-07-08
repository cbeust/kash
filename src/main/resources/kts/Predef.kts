
import java.io.File
import java.nio.file.Paths
import java.util.*

object Kash {
    val PATHS = arrayListOf<String>()
    val ENV = hashMapOf<String, String>()
    var PROMPT: String = ""
    val DIRS = Stack<String>()
}

fun path(vararg p: String) = p.forEach { Kash.PATHS.add(it) }
fun prompt(prompt: String) { Kash.PROMPT = prompt }
fun pwd() = Kash.DIRS.peek()

// These functions are called `kenv` so they don't collide with the `env` command that's probably on your path.
fun kenv(key: String, value: String) { Kash.ENV[key] = value }
fun kenv(key: String) = Kash.ENV[key]
val kenv: Map<String, String> get() = Kash.ENV

//
// The code below probably belongs more to a ~/.kash.kts, but leaving it here for now as an example
//

fun gitBranch() = File(pwd(), ".git/HEAD").let { head ->
    if (head.exists()) {
        val segments = head.readLines()[0].split("/")
        " [" + segments[segments.size - 1] + "]"
    } else {
        ""
    }
}

fun currentDir() = Paths.get(pwd()).let { path ->
    path.nameCount.let { size ->
        if (size > 2) path.getName(size - 2).toString() + "/" + path.getName(size - 1).toString()
        else path.toString()
    }
}

fun myPrompt() = currentDir() + gitBranch() + "\u001B[32m$ "

prompt("`myPrompt()`")
