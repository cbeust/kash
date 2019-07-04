
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.jar.JarInputStream

fun findInJars(name: String, dir: String = "."): Map<String, ArrayList<String>> {
    val result = hashMapOf<String, ArrayList<String>>()
    File(dir).listFiles()?.forEach {
        if (it.isFile && it.name.endsWith(".jar")) {
            val jis = JarInputStream(FileInputStream(it))
            var entry = jis.nextJarEntry
            while (entry != null) {
                if (entry.name.contains(name)) {
                    val key = it.absolutePath
                    val list = result[key] ?: arrayListOf()
                    if (! result.containsKey(key)) {
                        result[key] = list
                    }
                    list.add(entry.name)
                }
                entry = jis.nextJarEntry
            }
        } else {
            result.putAll(findInJars(name, it.absolutePath))
        }
    }
    return result
}

if (args.isEmpty()) {
    println("Usage: findInJars name [directory to search for jar files]")
} else {
    val result =
        if (args.size == 1) findInJars(args[0])
        else findInJars(args[0], args[1])
    result.entries.forEach { entry ->
        println(entry.key)
        entry.value.forEach { match ->
            println("  $match")
        }
    }
}
