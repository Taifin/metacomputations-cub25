plugins {
    kotlin("jvm") version "2.0.21"
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