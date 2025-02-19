package interpreter

import me.alllex.parsus.parser.*
import me.alllex.parsus.token.literalToken
import me.alllex.parsus.token.regexToken

class FlowChartGrammar : Grammar<Program>() {
    init {
        regexToken("\\s", ignored = true)
    }

    private val readToken by literalToken("read")
    private val lBracket by literalToken("(")
    private val rBracket by literalToken(")")
    private val comma by literalToken(",")
    private val semicolon by literalToken(";")

    private val identifier by regexToken("\\w+") map { Id(it.text) }
    private val identifiersList by separated(identifier, comma)

    private val read by -readToken * -lBracket * identifiersList * -rBracket * -semicolon map {
        Read(it)
    }

    private val stringLiteral by regexToken("\"[a-zA-Z]+\"") map { Literal(it.text.trim('\"')) }

    private val constant by regexToken("-?\\d+") map { Constant(it.text.toInt()) }
    private val opName by literalToken("hd") or literalToken("tl") or literalToken("cons") or literalToken("eq") map {
        it.text
    }

    private val op by parser {
        val name = opName()
        lBracket()
        val args = separated(expr, comma)()
        rBracket()
        Operation(name, args)
    }

    private val expr: Parser<Expr> by stringLiteral or constant or op or identifier

    private val gotoStatement by -literalToken("goto") * identifier * -semicolon map {
        Goto(it)
    }

    private val ifElseStatement by -literalToken("if") * expr * -literalToken("goto") * identifier *
            -literalToken("else") * identifier * -semicolon map {
        IfElse(it.t1, it.t2, it.t3)
    }

    private val returnStatement by -literalToken("return") * expr * -semicolon map {
        Return(it)
    }

    private val jump by gotoStatement or ifElseStatement or returnStatement

    private val assignment by identifier * -literalToken(":=") * expr * -semicolon map {
        Assignment(it.first, it.second)
    }

    private val basicBlock by identifier * -literalToken(":") * optional(repeated(assignment, 0)) * jump map {
        BasicBlock(it.t1, it.t2, it.t3)
    }

    override val root: Parser<Program> by read * repeated(basicBlock, 1) map {
        Program(it.first, it.second)
    }
}