import java.nio.file.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isWritable

class ProgPrettyPrinter {
    fun prettyPrint(prog: List<List<String>>, file: Path) {
        file.deleteIfExists()
        if (file.isWritable()) {
            throw IllegalArgumentException("Cannot write to path $file")
        }

        val writer = file.bufferedWriter()

        for (block in prog) {
            val label = block[0]
            val bb = block.drop(1)
            writer.write(label)
            bb.forEachIndexed { index, s ->
                if (index > 0) writer.write("\t\t$s\n")
                else writer.write("\t$s\n")
            }
            writer.write("\n")
        }

        writer.flush()
    }
}