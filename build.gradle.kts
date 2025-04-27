import java.nio.file.Paths
import kotlin.io.path.createDirectories

interface OutputDirectories {
    val firstProjection: String
    val secondProjection: String
    val thirdProjection: String
}

val outputDirectories = object : OutputDirectories {
    override val thirdProjection: String by lazy {
            val dir = "output/thirdProjection"
            Paths.get(dir).createDirectories()
            dir
        }

    override val secondProjection: String by lazy {
            val dir = "output/secondProjection"
            Paths.get(dir).createDirectories()
            dir
        }

    override val firstProjection: String by lazy {
            val dir = "output/firstProjection"
            Paths.get(dir).createDirectories()
            dir
        }
}

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

    implementation("com.xenomachina:kotlin-argparser:2.0.7")
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
        prog ${outputDirectories.firstProjection}/compiled.fchart Right
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
        list("program","division","programL","gotoLab","startPP","rest","pp","bb","command","fst","x","exp","tmp","pp0","pp1","liveVars")
        map(list("program",parse("fchart/turing-machine/turing-machine-int.fchart")),list("division",list("Q","Qtail","Instruction","Operator","Symbol","Nextlabel")))
        prog ${outputDirectories.secondProjection}/compiler.fchart vs
    """.trimIndent()

    args = listOf("fchart/mix.fchart", "--debug")
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
        prog ${outputDirectories.secondProjection}/compiled.fchart Right
    """.trimIndent()

    args = listOf("${outputDirectories.secondProjection}/compiler.fchart", "--debug")
    standardInput = mixTmInput.byteInputStream()
    mainClass.set(application.mainClass)
    classpath = sourceSets["main"].runtimeClasspath
    dependsOn("classes", "mixMixTm")
}

tasks.register<JavaExec>("mixMixMix") {
    group = "interpreter"
    description = "run mix on itself and itself, output to compiler-gen.fchart"

    val mixTmInput = """
        parse("fchart/mix.fchart")
        list("program","division","programL","gotoLab","startPP","rest","pp","bb","command","fst","x","exp","tmp","pp0","pp1","liveVars")
        map(list("program",parse("fchart/mix.fchart")),list("division",list("program","division","programL","gotoLab","startPP","rest","pp","bb","command","fst","x","exp","tmp","pp0","pp1")))
        prog ${outputDirectories.thirdProjection}/compiler-gen.fchart vs
    """.trimIndent()

    args = listOf("fchart/mix.fchart", "-L")
    standardInput = mixTmInput.byteInputStream()
    mainClass.set(application.mainClass)
    classpath = sourceSets["main"].runtimeClasspath
    dependsOn("classes")
}

tasks.register<JavaExec>("generateCompiler") {
    group = "interpreter"
    description = "run compiler generator on TM interpreter, save compiled program to compiler-3.fchart"

    val mixTmInput = """
        map(list("program",parse("fchart/turing-machine/turing-machine-int.fchart")),list("division",list("Q","Qtail","Instruction","Operator","Symbol","Nextlabel")))
        prog ${outputDirectories.thirdProjection}/compiler.fchart vs
    """.trimIndent()

    args = listOf("${outputDirectories.thirdProjection}/compiler-gen.fchart")
    standardInput = mixTmInput.byteInputStream()
    mainClass.set(application.mainClass)
    classpath = sourceSets["main"].runtimeClasspath
    dependsOn("classes", "mixMixMix")
}


tasks.register<JavaExec>("thirdProjection") {
    group = "interpreter"
    description =
        "run the compiler generated by third projection on an example turing-machine program, output to compiled-3.fchart"

    val mixTmInput = """
        map(list("Q",list(list("if","0","goto","3"),list("right"),list("goto","0"),list("write","1"))))
        prog ${outputDirectories.thirdProjection}/compiled.fchart Right
    """.trimIndent()

    args = listOf("${outputDirectories.thirdProjection}/compiler.fchart")
    standardInput = mixTmInput.byteInputStream()
    mainClass.set(application.mainClass)
    classpath = sourceSets["main"].runtimeClasspath
    dependsOn("classes", "generateCompiler")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

application {
    mainClass = "MainKt"
}