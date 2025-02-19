package interpreter

data class Read(val ids: List<Id>)

enum class Builtins {
    CONS,
    HEAD,
    TAIL,
    EQ,
}

sealed interface Expr

data class Id(val name: String) : Expr
data class Constant(val value: Int) : Expr
data class Literal(val value: String) : Expr
data class ListExpr(val items: List<Expr>) : Expr
data class Operation(val name: Builtins, val args: List<Expr>) : Expr

data class Goto(val label: Id) : Jump
data class IfElse(val cond: Expr, val trueBranch: Id, val falseBranch: Id) : Jump
data class Return(val expr: Expr) : Jump

sealed interface Jump

data class Assignment(val variable: Id, val value: Expr)

data class BasicBlock(val label: Id, val assignments: List<Assignment>?, val jump: Jump)

data class Program(val read: Read, val basicBlocks: List<BasicBlock>)

val RETURN = Id("__interpreter_internal_return__")