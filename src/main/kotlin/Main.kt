import me.alllex.parsus.parser.getOrThrow
import interpreter.FlowChartGrammar
import interpreter.Interpreter
import me.alllex.parsus.annotations.ExperimentalParsusApi
import me.alllex.parsus.parser.getOrElse
import java.nio.file.Paths
import kotlin.io.path.notExists
import kotlin.io.path.readText

@OptIn(ExperimentalParsusApi::class)
fun main(args: Array<String>) {
    val programFile = Paths.get(args[0])
    if (programFile.notExists()) {
        println("Input file does not exist!")
        return
    }

    val program = programFile.readText()
    val grammar = FlowChartGrammar()
    val res = grammar.parseTracingTokenMatching(program)

    val parsed = res.result.getOrElse {
        throw IllegalArgumentException(
            "Unable to read input: incorrect format\n${
                res.trace.events.joinToString("\n")
            }"
        )
    }
    val interpreter = Interpreter(parsed)
    println(interpreter.run())
}