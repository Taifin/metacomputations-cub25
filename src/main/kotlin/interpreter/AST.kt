package interpreter

data class Read(val ids: List<Id>)

enum class Builtins {
    CONS,
    HEAD,
    TAIL,
    LIST,
    MAP,
    EQ,
    FIRSTSYM,
    NEWTAIL,
    LOOKUPLABEL,
    LOOKUP,
    INITIALCODE,
    ISSTATIC,
    REDUCE,
    EVAL,
    SETDIFF,
    TOLIST,
    APPEND,
    APPENDCODE,
}

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
    override fun toString() = "${name.toString().lowercase()}(${args.joinToString(",")})"
}

data class Goto(val label: Id) : Jump {
    override fun toString() = "goto $label;"
}
data class IfElse(val cond: Expr, val trueBranch: Id, val falseBranch: Id) : Jump {
    override fun toString() = "if $cond goto $trueBranch else $falseBranch;"
}
data class Return(val expr: Expr) : Jump {
    override fun toString() = "return $expr;"
}

sealed interface Jump

data class Assignment(val variable: Id, val value: Expr) {
    override fun toString() = "$variable := $value"
}

data class BasicBlock(val label: Id, val assignments: List<Assignment>?, val jump: Jump)

data class Program(val read: Read, val basicBlocks: List<BasicBlock>)

val RETURN = Id("__interpreter_internal_return__")