plugins {
    kotlin("jvm") version "2.0.21"
}

group = "cub.taifin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("me.alllex.parsus:parsus-jvm:0.6.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}