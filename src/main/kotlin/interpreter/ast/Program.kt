package interpreter.ast

data class Read(val ids: List<Id>)

data class Assignment(val variable: Id, val value: Expr) {
    override fun toString() = "$variable := $value"
}

data class BasicBlock(val label: Label, val assignments: List<Assignment>?, val jump: Jump)

data class Program(val read: Read, val basicBlocks: List<BasicBlock>)
