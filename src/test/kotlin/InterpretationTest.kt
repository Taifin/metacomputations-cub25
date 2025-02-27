import interpreter.FlowChartGrammar
import interpreter.Interpreter
import me.alllex.parsus.parser.getOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.test.AfterTest

@Test
class InterpretationTest {
    private val stdin = System.`in`
    private val tmDir = Paths.get("src/test/resources/tm")
    private val tmInterpreter = "turing-machine-int.fchart"
    private val mix = Paths.get("src/test/resources/mix.fchart")

    @AfterTest
    fun tearDown() {
        System.setIn(stdin)
    }

    @Test
    fun `dictionary program`() {
        val grammar = FlowChartGrammar()
        val res = (grammar.parse(
            """
        read (name, namelist, valuelist);
        search: if eq (name, head (namelist)) goto found else cont;
        cont: valuelist := tail (valuelist);
              namelist := tail (namelist);
              goto search;
        found: return head (valuelist);""".trimIndent()
        ))

        val program = res.getOrThrow()

        val stream = """
            "x"
            list("z","y","x")
            list(1,3,1000)
        """.trimIndent()
        System.setIn(stream.byteInputStream())
        val interpreter = Interpreter(program)

        assertThat(interpreter.run()).isEqualTo(1000)
    }

    @Test
    fun `turing machine interpreter on trivial example`() {
        val grammar = FlowChartGrammar()
        val res = grammar.parse(tmDir.resolve(tmInterpreter).readText()).getOrThrow()

        val stream = tmDir.resolve("01-example.inp").readText()
        System.setIn(stream.byteInputStream())
        val interpreter = Interpreter(res)

        assertThat(interpreter.run()).isEqualTo(listOf("1", "1", "0", "1"))
    }

    @Test
    fun `first projection on turing machine trivial example`() {
        val grammar = FlowChartGrammar()
        val res = grammar.parse(mix.readText()).getOrThrow()

        val stream = "\$fchart/turing-machine/turing-machine-int.fchart\n" +
                "list(\"Q\",\"Qtail\",\"Instruction\",\"Operator\",\"Symbol\",\"Nextlabel\")\n" +
                "map(list(\"Q\",list(list(\"if\",\"0\",\"goto\",\"3\"),list(\"right\"),list(\"goto\",\"0\"),list(\"write\",\"1\"))))"
        System.setIn(stream.byteInputStream())
        val interpreter = Interpreter(res)

        val compiled = interpreter.run()
        assertThat(compiled).isEqualTo(
            listOf(
                listOf("lab0:", "Left := list();",  "if eq(\"0\",firstsym(Right)) goto lab1 else lab2;"),
                listOf("lab1:", "Right := cons(\"1\",tail(Right));", "return Right;"),
                listOf(
                    "lab2:",
                    "Left := cons(firstsym(Right),Left);",
                    "Right := tail(Right);",
                    "if eq(\"0\",firstsym(Right)) goto lab1 else lab2;"
                )
            )
        )
        val prg = "read (Right);" + (compiled as List<List<String>>).joinToString(" ") { it.joinToString(" ") }
        System.setIn("list(\"1\",\"1\",\"0\",\"1\",\"0\",\"1\")".byteInputStream())
        val prgParsed = grammar.parse(prg).getOrThrow()
        val newInterpreter = Interpreter(prgParsed)
        val tmRes = newInterpreter.run()
        assertThat(tmRes).isEqualTo(listOf("1", "1", "0", "1"))
    }
}