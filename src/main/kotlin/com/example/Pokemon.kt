package com.example

import com.example.sqldelight.PokemonDatabase
import com.example.sqldelight.migrations.PokemonEntity
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import kotlinx.coroutines.coroutineScope
import org.slf4j.MDC
import java.text.DateFormat
import java.util.*

interface Pokemon {
    val id: Int
    val name: String
    val color: Color

    enum class Color {
        RED, BLUE, YELLOW
    }
}

data class NormalPokemon(
    override val id: Int,
    override val name: String,
    override val color: Pokemon.Color
) : Pokemon

data class LegendPokemon(
    override val id: Int,
    override val name: String,
    override val color: Pokemon.Color,
    val location: String
) : Pokemon

private val queries = DatabaseSingleton.db.pokemonEntityQueries

class PokemonQuery : Query {
    private val logger = KotlinLogging.logger {}

    @GraphQLDescription("Get all Pokemon")
    @Suppress("unused")
    suspend fun getAllPokemon(): List<Pokemon> = coroutineScope {
        usingSpan("PokemonQuery", "getPokemonNyId") {
            logger.info { "getAllPokemon is called." }
            queries.selectAll().executeAsList().map { it.toPokemon() }
        }
    }

    @GraphQLDescription("Get a Pokemon by ID")
    @Suppress("unused")
    suspend fun getPokemonNyId(id: ID): Pokemon? = coroutineScope {
        usingSpan("PokemonQuery", "getPokemonNyId") { span ->
            withLoggingContext("id" to id.value) {
                MDC.put("id", id.value)
                logger.atInfo {
                    message = "getPokemonNyId is called."
                    payload = mapOf("id" to id)
                }
                logger.info { "getPokemonNyId is called. id:$id" }
                span.addEvent("getPokemonNyId is called.")

                queries.findById(id.value.toInt()).executeAsOneOrNull()?.toPokemon()
            }
        }
    }

    @Suppress("unused")
    suspend fun date(env: DataFetchingEnvironment): String = coroutineScope {
        val locale: Locale = env.graphQlContext.get("locale")
        logger.info("Locale is {locale}.", locale)
        val dataFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale)
        dataFormat.format(Date())
    }
}

class PokemonMutation : Mutation {
    private val logger = KotlinLogging.logger {}

    @GraphQLDescription("Add Pokemon")
    @Suppress("unused")
    suspend fun addPokemon(
        name: String,
        color: Pokemon.Color,
        location: String? = null
    ): Pokemon = coroutineScope {
        usingSpan("PokemonMutation", "addPokemon") {
            queries.transactionWithResult {
                afterCommit { logger.info { "commited." } }
                afterRollback { logger.info { "rollback." } }
                queries.insert(name, color.name, location).executeAsOne().toPokemon()
            }
        }
    }

    @GraphQLDescription("Database migration")
    @Suppress("unused")
    suspend fun migration(): String = coroutineScope {
        // 無理やり実行するならこんな感じ
        PokemonDatabase.Schema.migrate(
            DatabaseSingleton.sqlDriver,
            0,
            PokemonDatabase.Schema.version
        )
        "Migration completed."
    }
}

private fun PokemonEntity.toPokemon() = if (location != null) {
    LegendPokemon(id, name, Pokemon.Color.valueOf(color), location)
} else {
    NormalPokemon(id, name, Pokemon.Color.valueOf(color))
}

