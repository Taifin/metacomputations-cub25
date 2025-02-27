package interpreter

import me.alllex.parsus.annotations.ExperimentalParsusApi
import me.alllex.parsus.parser.*
import java.nio.file.Paths
import kotlin.io.path.notExists
import kotlin.io.path.readText

class Interpreter(private val program: Program) {
    private val vars = mutableMapOf<String, Any>()
    private val blocks = mutableMapOf<Id, BasicBlock>()
    private val exprParser = object : ExprGrammar<Expr>() {
        override val root: Parser<Expr> by expr
    }
    private val fchartParser = FlowChartGrammar()

    private var labInd = 0
    private fun generateLabel(): String = "lab${labInd++}"
    private val stateToLab = mutableMapOf<Pair<String, String>, String>()

    @OptIn(ExperimentalParsusApi::class)
    private fun readVars() {
        for (id in program.read.ids) {
            val inp = readlnOrNull() ?: throw IllegalArgumentException("Unable to read input: EOF reached")
            val mode = inp.first()
            val input = inp.drop(1)
            if (mode == '$') { // flowchart program
                val file = Paths.get(input)
                if (file.notExists()) {
                    throw IllegalArgumentException("Non existing file passed as an input!")
                }
                val parsed = fchartParser.parseTracingTokenMatching(file.readText())
                val result = parsed.result.getOrElse {
                    throw IllegalArgumentException(
                        "Unable to read input: incorrect format\n${
                            parsed.trace.events.joinToString("\n")
                        }"
                    )
                }
                vars[id.name] = result
            } else {
                val parsed = exprParser.parseTracingTokenMatching(inp)
                val result = parsed.result.getOrElse {
                    throw IllegalArgumentException(
                        "Unable to read input: incorrect format\n${
                            parsed.trace.events.joinToString("\n")
                        }"
                    )
                }
                vars[id.name] = evalExpr(result, vars)
            }
        }
    }

    private fun resolveLabels() {
        for (block in program.basicBlocks) {
            blocks[block.label] = block
        }
    }

    private fun toList(arg: Any): Any {
        return when (arg) {
            is Goto -> {
                listOf("goto", arg.label)
            }

            is IfElse -> {
                listOf("if", arg.cond, arg.trueBranch, arg.falseBranch)
            }

            is Return -> {
                listOf("return", arg.expr)
            }

            is Assignment -> {
                listOf(":=", arg.variable, arg.value)
            }

            is BasicBlock -> {
                mutableListOf<Any>().also { arg.assignments?.let { it1 -> it.addAll(it1) } }.also { it.add(arg.jump) }
            }

            is Program -> {
                mutableListOf<Any>(arg.read).also { it.addAll(arg.basicBlocks.map { it.label }) }
            }

            else -> throw IllegalArgumentException("Cannot be converted to list: $arg")
        }
    }

    private fun isStatic(exp: Expr, division: List<String>): Boolean {
        return when (exp) {
            is Constant -> true
            is Id -> exp.name in division
            is Literal -> true
            is Operation -> exp.args.all { isStatic(it, division) }
        }
    }

    private fun reduce(exp: Expr, vars: MutableMap<String, Any>): Any {
        when (exp) {
            is Constant -> return exp.value
            is Id -> {
                if (vars.containsKey(exp.name)) {
                    return vars[exp.name]!!
                }
                return exp
            }

            is Literal -> return exp.value
            is Operation -> {
                return if (exp.args.all { isStatic(it, vars.keys.toList()) }) {
                    evalExpr(exp, vars)
                } else {
                    exp
                }
            }
        }
    }

    private fun evalOp(op: Operation, vars: MutableMap<String, Any>): Any {
        val evaluatedArgs = op.args.map { evalExpr(it, vars) }
        when (op.name) {
            Builtins.CONS -> {
                val list = evaluatedArgs[1] as List<Any>
                val head = evaluatedArgs[0]
                return mutableListOf(head).also { it.addAll(list) }
            }

            Builtins.HEAD -> {
                val list = evaluatedArgs[0] as List<Any>
                return list.first()
            }

            Builtins.TAIL -> {
                val list = evaluatedArgs[0] as List<Any>
                if (list.isEmpty()) {
                    return list
                }
                return list.drop(1).toMutableList()
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

            Builtins.LOOKUP -> {
                val pp = evaluatedArgs[0]
                val program = evaluatedArgs[1] as Program
                for (block in program.basicBlocks) {
                    if (block.label == pp) {
                        return toList(block)
                    }
                }
                throw IllegalArgumentException("No such program point in the program!")
            }

            Builtins.INITIALCODE -> {
                val pp = evaluatedArgs[0] as Id
                val vs = evaluatedArgs[1]
                val key = pp.toString() to vs.toString()
                if (!stateToLab.containsKey(key)) {
                    val label = generateLabel()
                    stateToLab[key] = label
                }
                return mutableListOf("${stateToLab[key]!!}:")
            }

            Builtins.ISSTATIC -> {
                val arg = evaluatedArgs[0] as Expr
                val division = evaluatedArgs[1] as List<String>
                return isStatic(arg, division)
            }

            Builtins.REDUCE -> {
                val exp = evaluatedArgs[0] as Expr
                return reduce(exp, vars)
            }

            Builtins.APPEND -> {
                val collection = evaluatedArgs[0]
                val elem = evaluatedArgs[1]
                when (collection) {
                    is List<*> -> {
                        collection as MutableList<Any>
                        if (elem is List<*> && elem.isEmpty()) return collection
                        // TODO: this is to avoid adding empty list after setdiff
                        collection.add(elem)
                    }

                    is Map<*, *> -> {
                        collection as MutableMap<String, Any>
                        elem as List<Any>
                        collection[(elem[0] as Id).name] = elem[1]
                    }

                    else -> throw IllegalArgumentException("Argument is not a collection: $collection")
                }
                return collection
            }

            Builtins.APPENDCODE -> {
                val code = evaluatedArgs[0] as MutableList<Any>
                val new = evaluatedArgs[1] as String
                val replaced = new.replace(Regex("\\{(\\w+)}")) {
                    val key = it.groupValues[1]
                    vars[key]?.toString() ?: it.value
                }
                return code.also { it.add(replaced) }
            }

            Builtins.EVAL -> {
                val exp = evaluatedArgs[0] as Expr
                val vs = evaluatedArgs[1] as MutableMap<String, Any>
                return eval(exp, vs)
            }

            Builtins.SETDIFF -> {
                val pair = evaluatedArgs[0] as List<Any>
                val set = evaluatedArgs[1] as List<Any>
                return if (pair !in set) pair else mutableListOf()
            }

            Builtins.TOLIST -> {
                return toList(evaluatedArgs[0])
            }

            Builtins.MAP -> {
                val args = evaluatedArgs.associate {
                    it as List<Any>
                    it[0] to it[1]
                }
                return args
            }

            Builtins.LOOKUPLABEL -> {
                val pp = evaluatedArgs[0]
                val vs = evaluatedArgs[1]
                val key = pp.toString() to vs.toString()
                if (!stateToLab.containsKey(key)) {
                    stateToLab[key] = generateLabel()
                }
                return stateToLab[pp.toString() to vs.toString()]!!
            }
        }
    }

    private fun eval(exp: Expr, vs: MutableMap<String, Any>): Any {
        return when (exp) {
            is Constant -> exp.value
            is Id -> {
                val value = vs[exp.name] ?: throw IllegalArgumentException("Given ID is not static by division")
                if (value is MutableMap<*, *>) {
                    return value.toMutableMap() // explicit copy for vs in mix
                } else {
                    return value
                }
            }

            is Literal -> exp.value
            is Operation -> evalOp(exp, vs)
        }
    }

    private fun evalExpr(expr: Expr, variables: MutableMap<String, Any>): Any {
        return when (expr) {
            is Constant -> expr.value
            is Id -> {
                if (!variables.contains(expr.name)) {
                    variables[expr.name] = mutableListOf<Any>()
                }
                val value = variables[expr.name]!!
                if (value is MutableMap<*, *>) {
                    return value.toMutableMap() // explicit copy for vs in mix
                } else {
                    return value
                }
            }

            is Literal -> expr.value
            is Operation -> evalOp(expr, variables)
        }
    }

    private fun evalJump(jump: Jump): Id {
        return when (jump) {
            is Goto -> jump.label
            is IfElse -> {
                val cond = evalExpr(jump.cond, vars)
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
                vars[assignment.variable.name] = evalExpr(assignment.value, vars)
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
                return evalExpr((currentBlock.jump as Return).expr, vars)
            }

            currentLabel = newLabel
        }
    }
}