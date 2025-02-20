package interpreter

import me.alllex.parsus.annotations.ExperimentalParsusApi
import me.alllex.parsus.parser.*

class Interpreter(private val program: Program) {
    private val vars = mutableMapOf<Id, Any>()
    private val blocks = mutableMapOf<Id, BasicBlock>()
    private val exprParser = object : ExprGrammar<Expr>() {
        override val root: Parser<Expr> by expr
    }

    @OptIn(ExperimentalParsusApi::class)
    private fun readVars() {
        for (id in program.read.ids) {
            val inp = readlnOrNull() ?: throw IllegalArgumentException("Unable to read input: EOF reached")
            val parsed = exprParser.parseTracingTokenMatching(inp)
            val result = parsed.result.getOrElse { throw IllegalArgumentException("Unable to read input: incorrect format\n${parsed.trace.events.joinToString("\n")}") }
            vars[id] = evalExpr(result)
        }
    }

    private fun resolveLabels() {
        for (block in program.basicBlocks) {
            blocks[block.label] = block
        }
    }

    private fun evalOp(op: Operation): Any {
        val evaluatedArgs = op.args.map { evalExpr(it) }
        when (op.name) {
            Builtins.CONS -> {
                val list = evaluatedArgs[1] as? List<Any>
                val head = evaluatedArgs[0]
                return mutableListOf(head).also { it.addAll(list!!) }
            }

            Builtins.HEAD -> {
                val list = evaluatedArgs[0] as? List<Any>
                return list!!.first()
            }

            Builtins.TAIL -> {
                val list = evaluatedArgs[0] as List<Any>
                if (list.isEmpty()) {
                    return list
                }
                return list.drop(1)
            }

            Builtins.EQ -> {
                val lhs = evaluatedArgs[0]
                val rhs = evaluatedArgs[1]
                return lhs == rhs
            }

            Builtins.LIST -> {
                return evaluatedArgs
            }

            Builtins.FIRSTSYM -> {
                val list = evaluatedArgs[0] as List<Any>
                if (list.isEmpty()) {
                    return "B"
                }
                return list.first()
            }
            Builtins.NEWTAIL -> {
                val label = evaluatedArgs[0] as String
                val labelAsIndex = label.toInt()
                val q = evaluatedArgs[1] as List<Any>
                return q.drop(labelAsIndex)
            }
        }
    }

    private fun evalExpr(expr: Expr): Any {
        return when (expr) {
            is Constant -> expr.value
            is Id -> {
                if (!vars.contains(expr)) {
                    vars[expr] = emptyList<Any>()
                }
                vars[expr]!!
            }

            is Literal -> expr.value
            is Operation -> evalOp(expr)
        }
    }

    private fun evalJump(jump: Jump): Id {
        return when (jump) {
            is Goto -> jump.label
            is IfElse -> {
                val cond = evalExpr(jump.cond)
                if (cond as Boolean) jump.trueBranch else jump.falseBranch
            }

            is Return -> {
                RETURN
            }
        }
    }

    private fun runBlock(basicBlock: BasicBlock): Id {
        if (basicBlock.assignments != null)
            for (assignment in basicBlock.assignments) {
                vars[assignment.variable] = evalExpr(assignment.value)
            }
        return evalJump(basicBlock.jump)
    }

    fun run(): Any {
        readVars()
        resolveLabels()
        var currentLabel = program.basicBlocks.first().label
        while (true) {
            val currentBlock = blocks[currentLabel]!!
            val newLabel = runBlock(currentBlock)

            if (newLabel == RETURN) {
                return evalExpr((currentBlock.jump as Return).expr)
            }

            currentLabel = newLabel
        }
    }
}