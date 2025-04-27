package interpreter

import me.alllex.parsus.annotations.ExperimentalParsusApi
import me.alllex.parsus.parser.*
import interpreter.ast.*
import util.Log
import java.nio.file.Paths
import kotlin.io.path.readText

class Interpreter(private val program: Program, private val config: Config) {
    interface Config {
        val useFullLiveVarAnalysis: Boolean
        val isDebug: Boolean
    }

    private fun log(msg: String) {
        if (config.isDebug) {
            Log.log(msg)
        }
    }

    private val vars = mutableMapOf<String, Any>()
    private val blocks = mutableMapOf<Label, BasicBlock>()
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
            println("Input value of $id")
            val inp = readlnOrNull() ?: throw IllegalArgumentException("Unable to read input: EOF reached")
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
                arg.basicBlocks.map { listOf(it.label, toList(it)) }.toMutableList()
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

    private fun toExpr(value: Any, idToLit: Boolean = false): Expr {
        val exp = when (value) {
            is Int -> Constant(value)
            is String -> Literal(value)
            is Id -> if (idToLit) Literal(value.name) else value
            is Expr -> value
            is Label -> Literal(value.name)
            is List<*> -> Operation(Builtins.LIST, value.map { toExpr(it!!, idToLit) })
            is Set<*> -> Operation(Builtins.SET, value.map { toExpr(it!!, idToLit) })
            is Map<*, *> -> Operation(
                Builtins.MAP,
                value.entries.map { Operation(Builtins.LIST, listOf(toExpr(it.key!!, idToLit), toExpr(it.value!!, idToLit))) })

            else -> throw IllegalArgumentException("Value cannot be converted to expression: $value")
        }
        return exp
    }

    private fun eval(exp: Expr, vs: MutableMap<String, Any>): Any {
        return when (exp) {
            is Constant -> exp.value
            is Id -> {
                val value = vs[exp.name] ?: throw IllegalArgumentException("Given ID $exp is not static by division")
                if (value is MutableMap<*, *>) {
                    return value.toMutableMap() // explicit copy for vs in mix
                } else {
                    return value
                }
            }

            is Literal -> {
                return vs[exp.value] ?: throw IllegalArgumentException("Given ID $exp is not static by division")
            }
            is Operation -> evalOp(exp, vs)
        }
    }

    private fun reduce(exp: Expr, vars: MutableMap<String, Any>, idsToLit: Boolean = true): Expr {
        when (exp) {
            is Id -> {
                if (vars.containsKey(exp.name)) {
                    return toExpr(vars[exp.name]!!, idsToLit)
                }
                return exp
            }
            is Operation -> {
                return if (exp.args.all { isStatic(it, vars.keys.toList()) }) {
                    toExpr(evalExpr(exp, vars), idsToLit)
                } else {
                    if (exp.name == Builtins.EVAL || exp.name == Builtins.REDUCE) {
                        exp.copy(args = listOf(Literal(reduce(exp.args[0], vars, false).toString().replace("\"", "\\\"")), exp.args[1]))
                    } else {
                        exp.copy(args = exp.args.map { reduce(it, vars) })
                    }
                }
            }
            is Literal -> {
                return Literal(exp.value.replace(Regex("\\{(\\w+)}")) {
                    val key = it.groupValues[1]
                    vars[key]?.toString() ?: it.value
                })
            }
            else -> return exp
        }
    }

    @OptIn(ExperimentalParsusApi::class)
    private fun evalOp(op: Operation, vars: MutableMap<String, Any>): Any {
        val evaluatedArgs = op.args.map { evalExpr(it, vars) }
        when (op.name) {
            Builtins.CONS -> {
                val list = evaluatedArgs[1] as List<Any>
                val head = evaluatedArgs[0]
                // cons(setdiff(rest, pp), rest) -> avoid adding emptyList to rest
                if (head is List<*> && head.isEmpty()) return list.toMutableList()
                return mutableListOf(head).also { it.addAll(list) }
            }

            Builtins.HEAD -> {
                val list = evaluatedArgs[0] as List<Any>
                return list.first()
            }

            Builtins.TAIL -> {
                val list = evaluatedArgs[0] as List<Any>
                if (list.isEmpty()) {
                    return list.toMutableList()
                }
                return list.drop(1).toMutableList()
            }

            Builtins.NEXTLABEL -> {
                val pp = evaluatedArgs[0]
                val program = evaluatedArgs[1] as Program
                val list = evaluatedArgs[2] as List<Any>
                val label: Label = if (pp is String) Label(pp) else pp as Label
                val labels = program.basicBlocks.map { it.label }
                var ind = labels.indexOf(label) + 1
                while (ind < labels.size) {
                    list.forEach {
                        val cur: Label = if (it is String) Label(it) else it as Label
                        if (it == labels[ind]) {
                            return cur
                        }
                    }
                    ind++
                }
                log("No next label found for $pp")
                return if (pp is String) "error" else Label("error")
            }

            Builtins.EQ -> {
                val lhs = evaluatedArgs[0]
                val rhs = evaluatedArgs[1]
                return lhs == rhs
            }

            Builtins.LIST -> {
                return evaluatedArgs
            }

            Builtins.SET -> {
                return evaluatedArgs.toSet()
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
                throw IllegalArgumentException("No such program point $pp in the program!")
            }

            Builtins.INITIALCODE -> {
                val pp = evaluatedArgs[0]
                val vs = evaluatedArgs[1]
                val key = pp.toString() to vs.toString()
                if (!stateToLab.containsKey(key)) {
                    val label = generateLabel()
                    stateToLab[key] = label
                }
                return mutableListOf("${stateToLab[key]!!}:")
            }

            Builtins.ADDTOSTATE -> {
                val state = evaluatedArgs[0] as Map<String, Any>
                return state
            }

            Builtins.ISSTATIC -> {
                val arg = evaluatedArgs[0] as Expr
                val division = evaluatedArgs[1] as List<String>
                return isStatic(arg, division)
            }

            Builtins.REDUCE -> {
                val exp = when (val arg0 = evaluatedArgs[0]) {
                    is String -> exprParser.parseOrThrow(arg0)
                    else -> arg0 as Expr
                }
                val variables = evaluatedArgs[1] as MutableMap<String, Any>
                return reduce(exp, variables)
            }

            Builtins.EVAL -> {
                val exp = when (val arg0 = evaluatedArgs[0]) {
                    is String -> exprParser.parseOrThrow(arg0)
                    else -> arg0 as Expr
                }
                val vs = evaluatedArgs[1] as MutableMap<String, Any>
                return eval(exp, vs)
            }

            Builtins.APPEND -> {
                val collection = evaluatedArgs[0]
                val elem = evaluatedArgs[1]
                when (collection) {
                    is List<*> -> {
                        collection as MutableList<Any>
                        if (elem is List<*> && elem.isEmpty()) return collection
                        // this is to avoid adding emptyList after setdiff
                        return collection.toMutableList().also { it.add(elem) }
                    }

                    is Map<*, *> -> {
                        collection as MutableMap<String, Any>
                        elem as List<Any>
                        if (elem[0] == Id("rest")) {
                            println()
                        }
                        val copy = collection.toMutableMap()
                        when (val id = elem[0]) {
                            is Id -> copy[id.name] = elem[1]
                            else -> copy[id as String] = elem[1]
                        }
                        return copy
                    }

                    else -> throw IllegalArgumentException("Argument is not a collection: $collection")
                }
            }

            Builtins.APPENDCODE -> {
                val code = evaluatedArgs[0] as MutableList<Any>
                val new = evaluatedArgs[1] as String
                val replaced = new.replace(Regex("\\{(\\w+)}")) {
                    val key = it.groupValues[1]
                    vars[key]?.toString() ?: it.value
                }
                return code.also { it.add(replaced) }.toMutableList()
            }



            Builtins.SETDIFF -> {
                val pair = evaluatedArgs[0]
                val set = evaluatedArgs[1] as List<Any>
                return if (pair !in set) pair else mutableListOf<Any>()
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
                    stateToLab[key] = generateLabel() // + "_" + pp.toString() + "_" + vs.toString()
                }
                return stateToLab[pp.toString() to vs.toString()]!!
            }

            Builtins.PARSE -> {
                val filename = evaluatedArgs[0] as String
                val parsed = fchartParser.parseTracingTokenMatching(Paths.get(filename).readText())
                val result = parsed.result.getOrElse {
                    throw IllegalArgumentException(
                        "Unable to parse: incorrect format\n${
                            parsed.trace.events.joinToString("\n")
                        }"
                    )
                }
                return result
            }

            Builtins.FINDPROJECTIONS -> {
                val program = evaluatedArgs[0] as Program
                val division = evaluatedArgs[1] as List<String>
                val liveVars = LiveVariableAnalysis.analyse(config, program, division)
                val mapped = liveVars.mapValues { it.value.intersect(division.map { Id(it) }.toSet()) }
                return mapped
            }

            Builtins.COMPRESSSTATE -> {
                // the compiled mix uses strings instead of labels and ids
                val arg0 = evaluatedArgs[0]
                if (arg0 is String) {
                    val pp = arg0
                    val state = evaluatedArgs[1] as Map<String, Any>
                    val liveVars = evaluatedArgs[2] as Map<String, Set<String>>
                    val varsAtPp = liveVars[pp]!!
                    return state.filterKeys { key -> varsAtPp.contains(key) }.toMutableMap()
                } else {
                    val pp = arg0 as Label
                    val state = evaluatedArgs[1] as Map<String, Any>
                    val liveVars = evaluatedArgs[2] as Map<Label, Set<Id>>
                    val varsAtPp = liveVars[pp]!!
                    val compressed = state.filterKeys { key -> varsAtPp.contains(Id(key)) }
                    return compressed.toMutableMap()
                }
            }

            Builtins.APPENDPENDINGUNIQUE -> {
                val pending = evaluatedArgs[0] as MutableList<List<Any>>
                val ppState = evaluatedArgs[1] as List<Any>
                val state = ppState[1] as Map<String, Any>
                val marked = evaluatedArgs[2] as List<List<Any>>
                val pp = ppState[0]

                if (pp is Label) {
                    val cPending = pending.map {
                        it[0] as Label to it[1] as Map<String, Any>
                    }
                    val cMarked = marked.map {
                        it[0] as Label to it[1] as Map<String, Any>
                    }
                    val liveVars = evaluatedArgs[3] as Map<Label, Set<Id>>
                    if (containsWithCompressedState(pp, state, cPending, liveVars)) {
                        return pending
                    }
                    if (containsWithCompressedState(pp, state, cMarked, liveVars)) {
                        return pending
                    }
                    return mutableListOf(mutableListOf(pp, state)) + pending
                } else {
                    val cPending = pending.map {
                        Label(it[0] as String) to it[1] as Map<String, Any>
                    }
                    val cMarked = marked.map {
                        Label(it[0] as String) to it[1] as Map<String, Any>
                    }
                    val liveVars = evaluatedArgs[3] as Map<String, Set<String>>
                    val cLiveVars = liveVars.map { (key, value) ->
                        Label(key) to value.map { Id(it) }.toSet()
                    }.toMap()
                    if (containsWithCompressedState(Label(pp as String), state, cPending, cLiveVars)) {
                        return pending
                    }
                    if (containsWithCompressedState(Label(pp as String), state, cMarked, cLiveVars)) {
                        return pending
                    }
                    return mutableListOf(mutableListOf(pp, state)) + pending
                }
            }
        }
    }

    private fun containsWithCompressedState(pp: Label, state: Map<String, Any>, list: List<Pair<Label, Map<String, Any>>>, liveVars: Map<Label, Set<Id>>): Boolean {
        val varsAtPp = liveVars[pp]!!
        val stateCompressed = state.filterKeys { key: String -> varsAtPp.contains(Id(key)) }
        val listCompressed = list.map { (mPP, mState) ->
            mPP to mState.filterKeys { key: String -> liveVars[mPP]!!.contains(Id(key)) }
        }
        return (pp to stateCompressed) in listCompressed
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
                    return value.toMutableMap()
                } else {
                    return value
                }
            }

            is Literal -> expr.value
            is Operation -> evalOp(expr, variables)
        }
    }

    private fun evalJump(jump: Jump): Label {
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

    private fun runBlock(basicBlock: BasicBlock): Label {
        if (basicBlock.assignments != null) {
            for (assignment in basicBlock.assignments) {
                vars[assignment.variable.name] = evalExpr(assignment.value, vars)
                if (assignment.variable.name == "command" && assignment.value is Operation && assignment.value.name == Builtins.TOLIST) {
                    log("Processing inner instruction: ${vars[assignment.variable.name]}")
                }
            }
        }
        return evalJump(basicBlock.jump)
    }

    fun run(): Any {
        readVars()
        resolveLabels()
        var currentLabel = program.basicBlocks.first().label
        while (true) {
            log("At label $currentLabel")
            val currentBlock = blocks[currentLabel]!!
            val newLabel = runBlock(currentBlock)

            if (newLabel == RETURN) {
                return evalExpr((currentBlock.jump as Return).expr, vars)
            }

            currentLabel = newLabel
        }
    }
}