package interpreter.ast

enum class Builtins(val id: String) {
    CONS("cons"),
    HEAD("head"),
    TAIL("tail"),
    LIST("list"),
    MAP("map"),
    SET("set"),
    EQ("eq"),
    ADDTOSTATE("addToState"),
    FIRSTSYM("firstsym"),
    NEWTAIL("newtail"),
    NEXTLABEL("nextLabel"),
    LOOKUPLABEL("lookupLabel"),
    LOOKUP("lookup"),
    INITIALCODE("initialCode"),
    ISSTATIC("isStatic"),
    REDUCE("reduce"),
    EVAL("eval"),
    SETDIFF("setdiff"),
    TOLIST("toList"),
    APPENDPENDINGUNIQUE("appendPendingUnique"),
    APPEND("append"),
    APPENDCODE("appendCode"),
    PARSE("parse"),
    FINDPROJECTIONS("findProjections"),
    COMPRESSSTATE("compressState")
}

