package com.example

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.example.sqldelight.PokemonDatabase
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*

object DatabaseSingleton {

    internal lateinit var sqlDriver: SqlDriver private set
    internal lateinit var db: PokemonDatabase private set

    fun init(config: ApplicationConfig) {
        // どかっとまとめて渡せるはずなんだけど一旦横着する
        val driverClassName = config.property("database.driverClassName").getString()
        val jdbcURL = config.property("database.jdbcURL").getString()
        val username = config.property("database.username").getString()
        val password = config.property("database.password").getString()

        val dataSource = createHikariDataSource(jdbcURL, driverClassName, username, password)
        sqlDriver = dataSource.asJdbcDriver()
        db = PokemonDatabase(sqlDriver)
    }

    private fun createHikariDataSource(
        url: String,
        driver: String,
        username: String,
        password: String,
    ) = HikariDataSource(HikariConfig().apply {
        this.driverClassName = driver
        this.jdbcUrl = url
        this.maximumPoolSize = 3
        this.isAutoCommit = true // SqlDelight がトランザクションを自前で管理するので true じゃないとダメ
        this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        this.username = username
        this.password = password
        validate()
    })
}
