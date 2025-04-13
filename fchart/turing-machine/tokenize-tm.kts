import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readLines
import kotlin.io.path.writeText

val program: Path = Paths.get(args[0])
val tape: String = args[1]
val output: Path = program.parent.resolve(program.nameWithoutExtension + ".inp")
val lines = mutableListOf<String>()
for (line in program.readLines()) {
    val tokens = line.split(" ")
    lines.add("list(" + tokens.joinToString(",") { "\"$it\"" } + ")")
}
output.writeText("list(${lines.joinToString(",")})\nlist(${tape.map { "\"$it\"" }.joinToString(",")})")