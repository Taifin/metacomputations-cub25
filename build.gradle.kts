plugins {
    kotlin("jvm") version "2.0.21"
    id("application")
}

group = "cub.taifin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.testng:testng:7.4.0")
    testImplementation("org.assertj:assertj-core:3.25.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    implementation("me.alllex.parsus:parsus-jvm:0.6.1")
}

tasks.named<Test>("test") {
    useTestNG()
}

kotlin {
    jvmToolchain(8)
}

tasks.register<JavaExec>("firstProjection") {
    group = "interpreter"
    description = "run mix on turing machine interpreter and example program, outputs results to compiled-1.fchart"

    val mixTmInput = """
        parse("fchart/turing-machine/turing-machine-int.fchart")
        list("Q","Qtail","Instruction","Operator","Symbol","Nextlabel")
        map(list("Q",list(list("if","0","goto","3"),list("right"),list("goto","0"),list("write","1"))))
        prog compiled-1.fchart Right
    """.trimIndent()

    args = listOf("fchart/mix.fchart")
    standardInput = mixTmInput.byteInputStream()
    mainClass.set(application.mainClass)
    classpath = sourceSets["main"].runtimeClasspath
    dependsOn("classes")
}

tasks.register<JavaExec>("mixMixTm") {
    group = "interpreter"
    description = "run mix on itself and turing machine interpreter, outputs results to compiler.fchart"

    val mixTmInput = """
        parse("fchart/mix.fchart")
        list("program","division","programL","startPP","rest","lRest","pp","bb","command","fst","x","exp","tmp","pp0","pp1")
        map(list("program",parse("fchart/turing-machine/turing-machine-int.fchart")),list("division",list("Q","Qtail","Instruction","Operator","Symbol","Nextlabel")))
        prog compiler.fchart vs
    """.trimIndent()

    args = listOf("fchart/mix.fchart")
    standardInput = mixTmInput.byteInputStream()
    mainClass.set(application.mainClass)
    classpath = sourceSets["main"].runtimeClasspath
    dependsOn("classes")
}

tasks.register<JavaExec>("secondProjection") {
    group = "interpreter"
    description = "run the compiler on an example turing machine program, output resulting program to compiled-2.fchart"

    val mixTmInput = """
        map(list("Q",list(list("if","0","goto","3"),list("right"),list("goto","0"),list("write","1"))))
        prog compiled-2.fchart Read
    """.trimIndent()

    args = listOf("compiler.fchart")
    standardInput = mixTmInput.byteInputStream()
    mainClass.set(application.mainClass)
    classpath = sourceSets["main"].runtimeClasspath
    dependsOn("classes", "mixMixTm")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

application {
    mainClass = "MainKt"
}