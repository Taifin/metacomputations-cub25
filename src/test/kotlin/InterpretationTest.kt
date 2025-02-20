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
}