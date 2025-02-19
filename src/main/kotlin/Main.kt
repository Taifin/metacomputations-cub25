import me.alllex.parsus.parser.getOrThrow
import interpreter.FlowChartGrammar

fun main() {
    val grammar = FlowChartGrammar()
    val res = (grammar.parse("""
        read (name, namelist, valuelist);
        search: if eq (name, hd (namelist)) goto found else cont;
        cont: valuelist := tl (valuelist);
              namelist := tl (namelist);
              goto search;
        found: return hd (valuelist);""".trimIndent()))
    println(res.getOrThrow())
}