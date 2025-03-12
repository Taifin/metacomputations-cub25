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

    private fun collectUseDef(block: BasicBlock): LiveSets {
        val use = mutableSetOf<Id>()
        val def = mutableSetOf<Id>()
        for (assign in block.assignments ?: emptyList()) {
            def.add(assign.variable)
            use.addAll(usedVariables(assign.value))
        }

        when (block.jump) {
            is Return -> use.addAll(usedVariables(block.jump.expr))
            is IfElse -> use.addAll(usedVariables(block.jump.cond))
            else -> {}
        }

        return LiveSets(use.filterNot { it in def }.toSet(), def)
    }

    private fun buildCFG(program: Program) = program.basicBlocks.associateBy(
        keySelector = { it.label },
        valueTransform = {
            val successors = when (it.jump) {
                is Return -> emptyList()
                is Goto -> listOf(it.jump.label)
                is IfElse -> listOf(it.jump.trueBranch, it.jump.falseBranch)
            }
            CFGNode(it, successors)
        }
    )

    private fun reassignedVals(program: Program) = program.basicBlocks.flatMap { it.assignments?.map { it.variable } ?: emptyList() }.toSet()

    fun analyse(program: Program): Map<Label, Set<Id>> {
        val cfg = buildCFG(program)
        val changingVars = reassignedVals(program)
        val useDefMap = cfg.mapValues { collectUseDef(it.value.block) }

        val inMap = mutableMapOf<Label, Set<Id>>()
        val outMap = mutableMapOf<Label, Set<Id>>()
        cfg.keys.forEach {
            inMap[it] = emptySet()
            outMap[it] = emptySet()
        }

        var changed: Boolean
        do {
            changed = false
            for ((label, node) in cfg) {
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

        return inMap.mapValues { it.value.intersect(changingVars) }
    }
}