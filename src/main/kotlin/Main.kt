import me.alllex.parsus.parser.getOrThrow
import interpreter.FlowChartGrammar
import interpreter.Interpreter

fun main() {
    val grammar = FlowChartGrammar()
    val res = (grammar.parse("""
        read (name, namelist, valuelist);
        search: if eq (name, head (namelist)) goto found else cont;
        cont: valuelist := tail (valuelist);
              namelist := tail (namelist);
              goto search;
        found: return head (valuelist);""".trimIndent()))

    val program = res.getOrThrow()
    val interpreter = Interpreter(program)
    println(interpreter.run())
}