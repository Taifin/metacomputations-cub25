import interpreter.FlowChartGrammar
import interpreter.Interpreter
import interpreter.Log
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

    if (args.size >= 2 && args[1] == "--debug") {
        Log.enabled = true
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
    val result = interpreter.run()
    println("What to do with the interpreted results? (print|prog <file>)")
    val mode = readln()
    if (mode == "print") {
        println(result)
    } else if (mode.startsWith("prog")) {
        val file = mode.split(" ")[1]
        val path = Paths.get(file)
        val prettyPrinter = ProgPrettyPrinter()
        prettyPrinter.prettyPrint(result as List<List<String>>, path)
    }
}