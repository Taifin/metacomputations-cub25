package interpreter

import interpreter.ast.*

object LiveVariableAnalysis {
    data class CFGNode(val block: BasicBlock, val successors: List<Label>)
    data class LiveSets(val use: Set<Id>, val def: Set<Id>)

    private fun usedVariables(expr: Expr): List<Id> {
        return when (expr) {
            is Constant -> emptyList()
            is Id -> listOf(expr)
            is Literal -> emptyList()
            is Operation -> expr.args.flatMap { usedVariables(it) }
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

    private fun collectUseDef(block: BasicBlock): LiveSets {
        val use = mutableSetOf<Id>()
        val def = mutableSetOf<Id>()
        for (assign in block.assignments ?: emptyList()) {
            usedVariables(assign.value).forEach {
                if (it !in def) {
                    use.add(it)
                }
            }
            def.add(assign.variable)
        }

        usedVariables(when (block.jump) {
            is Return -> block.jump.expr
            is IfElse -> block.jump.cond
            else -> Constant(0)
        }).forEach {
            if (it !in def) {
                use.add(it)
            }
        }

        return LiveSets(use, def)
    }

    private fun buildCFG(program: Program, division: List<String>) = program.basicBlocks.associateBy(
        keySelector = { it.label },
        valueTransform = {
            val successors = when (it.jump) {
                is Return -> emptyList()
                is Goto -> listOf(it.jump.label)
                is IfElse -> {
                    if (isStatic(it.jump.cond, division)) {
                        listOf(it.jump.trueBranch, it.jump.falseBranch)
                    } else {
                        emptyList()
                    }
                }
            }
            CFGNode(it, successors)
        }
    )

    fun analyse(config: Interpreter.Config, program: Program, division: List<String>): Map<Label, Set<Id>> {
        val cfg = buildCFG(program, division)
        val useDefMap = cfg.mapValues { collectUseDef(it.value.block) }

        if (!config.useFullLiveVarAnalysis) {
            return useDefMap.mapValues { it.value.use }
        }

        val inMap = mutableMapOf<Label, Set<Id>>()
        val outMap = mutableMapOf<Label, Set<Id>>()
        cfg.keys.forEach {
            inMap[it] = emptySet()
            outMap[it] = emptySet()
        }

        var changed: Boolean
        do {
            changed = false
            for (label in cfg.keys.reversed()) {
                val node = cfg[label]!!
                val oldIn = inMap[label]!!
                val oldOut = outMap[label]!!

                val newOut = node.successors.flatMap { inMap[it] ?: emptySet() }.toSet()
                val (use, def) = useDefMap[label]!!
                val newIn = use + (newOut - def)

                if (newIn != oldIn || newOut != oldOut) {
                    inMap[label] = newIn
                    outMap[label] = newOut
                    changed = true
                }
            }
        } while (changed)

        return inMap.mapValues { it.value }
    }
}