@file:Suppress("PropertyName")

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val graphql_kotlin_version: String by project

plugins {
    kotlin("jvm") version "1.9.23"
    id("io.ktor.plugin") version "2.3.10"
    id("org.graalvm.buildtools.native") version "0.9.3"
    id("com.expediagroup.graphql") version "7.1.0"
}

group = "com.example"
version = "0.0.1"

application {
    mainClass.set("com.example.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.expediagroup", "graphql-kotlin-ktor-server", graphql_kotlin_version)
    implementation("io.ktor", "ktor-server-netty-jvm")
    implementation("ch.qos.logback", "logback-classic", logback_version)
    testImplementation("io.ktor", "ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin", "kotlin-test-junit", kotlin_version)
}

graphql {
    graalVm {
        packages = listOf("com.example")
    }
}

graalvmNative {
    toolchainDetection.set(false)
    binaries {
        named("main") {
            verbose.set(true)
            buildArgs.add("--initialize-at-build-time=io.ktor,kotlin,ch.qos.logback,org.slf4j")
            buildArgs.add("-H:+ReportExceptionStackTraces")
        }
        metadataRepository {
            enabled.set(true)
        }
    }
}