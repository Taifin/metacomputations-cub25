import interpreter.FlowChartGrammar
import interpreter.Interpreter
import me.alllex.parsus.parser.getOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import kotlin.test.AfterTest

@Test
class InterpretationTest {
    private val stdin = System.`in`

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
}