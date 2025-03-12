package interpreter.ast

data class Label(val name: String) {
    override fun toString(): String = name

    constructor(id: Id) : this(id.name)
}
data class Goto(val label: Label) : Jump {
    override fun toString() = "goto $label;"
}
data class IfElse(val cond: Expr, val trueBranch: Label, val falseBranch: Label) : Jump {
    override fun toString() = "if $cond goto $trueBranch else $falseBranch;"
}
data class Return(val expr: Expr) : Jump {
    override fun toString() = "return $expr;"
}

sealed interface Jump

val RETURN = Label("__interpreter_internal_return__")
