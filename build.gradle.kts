@file:Suppress("PropertyName")

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val graphql_kotlin_version: String by project
val hikaricp_version: String by project

plugins {
    application
    kotlin("jvm") version "1.9.23"
    id("io.ktor.plugin") version "2.3.10"
    id("com.expediagroup.graphql") version "7.1.0"
    id("app.cash.sqldelight") version "2.0.2"
}

group = "com.example"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.expediagroup", "graphql-kotlin-ktor-server", graphql_kotlin_version)
    implementation("io.ktor", "ktor-server-netty-jvm")
    testImplementation("io.ktor", "ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin", "kotlin-test-junit", kotlin_version)
    implementation("app.cash.sqldelight", "jdbc-driver", "2.0.2")
    implementation("com.zaxxer", "HikariCP", hikaricp_version)
    implementation("org.postgresql", "postgresql", "42.7.3")
    implementation("io.klogging:slf4j-klogging:0.5.11")
}

sqldelight {
    databases {
        create("PokemonDatabase") {
            packageName.set("com.example.sqldelight")
            dialect("app.cash.sqldelight:postgresql-dialect:2.0.2")
            srcDirs("src/main/sqldelight")
            deriveSchemaFromMigrations.set(true)
            schemaOutputDirectory.set(file("sqldelight/migrations/database"))
            migrationOutputDirectory = file("sqldelight/migrations")
        }
    }
}
