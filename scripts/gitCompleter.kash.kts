fun gitComplete(line: String, cursorIndex: Int): List<String> {
    val words = line.split(" ")
    if (words[0] == "git") return listOf("commit", "status")
    else return emptyList()
}

val result = if (args.size == 2) gitComplete(args[0], args[1].toInt())
else emptyList()

result
