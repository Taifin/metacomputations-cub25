package interpreter

sealed interface AstElement

data class Read(val ids: List<Id>) : AstElement

enum class Builtins(val id: String) {
    CONS("cons"),
    HEAD("head"),
    TAIL("tail"),
    LIST("list"),
    MAP("map"),
    EQ("eq"),
    FIRSTSYM("firstsym"),
    NEWTAIL("newtail"),
    LOOKUPLABEL("lookupLabel"),
    LOOKUP("lookup"),
    INITIALCODE("initialCode"),
    ISSTATIC("isStatic"),
    REDUCE("reduce"),
    EVAL("eval"),
    SETDIFF("setdiff"),
    TOLIST("toList"),
    APPEND("append"),
    APPENDCODE("appendCode"),
    PARSE("parse"),
}

sealed interface Expr

data class Id(val name: String) : Expr {
    override fun toString() = name
}
data class Label(val name: String) {
    override fun toString(): String = name

    constructor(id: Id) : this(id.name)
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

data class Assignment(val variable: Id, val value: Expr) {
    override fun toString() = "$variable := $value"
}

data class BasicBlock(val label: Label, val assignments: List<Assignment>?, val jump: Jump)

data class Program(val read: Read, val basicBlocks: List<BasicBlock>)

val RETURN = Label("__interpreter_internal_return__")