package interpreter

import me.alllex.parsus.parser.*
import me.alllex.parsus.token.literalToken
import me.alllex.parsus.token.regexToken

abstract class ExprGrammar<T> : Grammar<T>() {

    private val stringLiteral by regexToken("\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\"") map { Literal(it.text.trim('\"').replace("\\\"", "\"")) }

    private val constant by regexToken("-?\\d+") map { Constant(it.text.toInt()) }

    private val listToken by literalToken("list")

    protected val opName by literalToken("head") or
            literalToken("tail") or
            literalToken("cons") or
            literalToken("toList") or
            literalToken("list") or
            literalToken("map") or
            literalToken("eq") or
            literalToken("firstsym") or
            literalToken("newtail") or
            literalToken("lookupLabel") or
            literalToken("lookup") or
            literalToken("initialCode") or
            literalToken("isStatic") or
            literalToken("reduce") or
            literalToken("eval") or
            literalToken("setdiff") or
            literalToken("parse") or
            literalToken("appendCode") or
            literalToken("append") map {
        enumValueOf<Builtins>(it.text.uppercase())
    }

    protected val op by parser {
        val name = opName()
        lBracket()
        val args = separated(expr, comma)()
        rBracket()
        Operation(name, args)
    }

    protected val comma by literalToken(",")
    protected val lBracket by literalToken("(")
    protected val rBracket by literalToken(")")
    protected val identifier by regexToken("\\w+") map { Id(it.text) }

    // todo forbid identifier as input
    protected val expr: Parser<Expr> by constant or op or stringLiteral or identifier
}

class FlowChartGrammar : ExprGrammar<Program>() {
    init {
        regexToken("\\s", ignored = true)
    }

    private val readToken by literalToken("read")

    private val semicolon by literalToken(";")

    private val identifiersList by separated(identifier, comma)

    private val read by -readToken * -lBracket * identifiersList * -rBracket * -semicolon map {
        Read(it)
    }


    private val gotoStatement by -literalToken("goto") * identifier * -semicolon map {
        Goto(Label(it))
    }

    private val ifElseStatement by -literalToken("if") * expr * -literalToken("goto") * identifier *
            -literalToken("else") * identifier * -semicolon map {
        IfElse(it.t1, Label(it.t2), Label(it.t3))
    }

    private val returnStatement by -literalToken("return") * expr * -semicolon map {
        Return(it)
    }

    private val jump by gotoStatement or ifElseStatement or returnStatement

    private val assignment by identifier * -literalToken(":=") * expr * -semicolon map {
        Assignment(it.first, it.second)
    }

    private val basicBlock by identifier * -literalToken(":") * optional(repeated(assignment, 0)) * jump map {
        BasicBlock(Label(it.t1), it.t2, it.t3)
    }

    override val root: Parser<Program> by read * repeated(basicBlock, 1) map {
        Program(it.first, it.second)
    }
}