package interpreter.ast
sealed interface Expr

data class Id(val name: String) : Expr {
    override fun toString() = name
}

data class Constant(val value: Int) : Expr {
    override fun toString() = value.toString()
}
data class Literal(val value: String) : Expr {
    override fun toString() = "\"$value\""
}
data class Operation(val name: Builtins, val args: List<Expr>) : Expr {
    override fun toString() = "${name.id}(${args.joinToString(",")})"
}

