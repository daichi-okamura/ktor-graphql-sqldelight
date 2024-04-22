package com.example

import com.example.sqldelight.PokemonDatabase
import com.example.sqldelight.migrations.PokemonEntity
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.scalars.ID
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import kotlinx.coroutines.coroutineScope
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
        logger.info { "getAllPokemon is called." }
        val span = startSpan("selectAll")
        try {
            queries.selectAll().executeAsList().map { it.toPokemon() }.also {
                span.ok()
            }
        } catch (e: Exception) {
            span.error(e)
            throw e
        } finally {
            span.end()
        }
    }

    @GraphQLDescription("Get a Pokemon by ID")
    @Suppress("unused")
    suspend fun getPokemonNyId(id: ID): Pokemon? = coroutineScope {
        logger.atInfo {
            message = "getPokemonNyId is called."
            payload = mapOf("id" to id)
        }
        logger.info { "getPokemonNyId is called. id:$id" }

        val span = startSpan("findById")
        try {
            queries.findById(id.value.toInt()).executeAsOneOrNull()?.toPokemon().also {
                span.ok()
            }
        } catch (e: Exception) {
            span.error(e)
            throw e
        } finally {
            span.end()
        }
    }

    @Suppress("unused")
    suspend fun date(env: DataFetchingEnvironment): String = coroutineScope {
        val locale: Locale = env.graphQlContext.get("locale")
        logger.info("Locale is {locale}.", locale)
        val dataFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale)
        dataFormat.format(Date())
    }

    companion object {
        private fun startSpan(spanName: String): Span {
            return GlobalOpenTelemetry
                .getTracer("PokemonQuery")
                .spanBuilder(spanName)
                .startSpan()
        }
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
        val span = startSpan("insert")
        try {
            queries.transactionWithResult {
                afterCommit { logger.info { "commited." } }
                afterRollback { logger.info { "rollback." } }
                queries.insert(name, color.name, location).executeAsOne().toPokemon()
            }.also {
                span.ok()
            }
        } catch (e: Exception) {
            span.error(e)
            throw e
        } finally {
            span.end()
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

    companion object {
        private fun startSpan(spanName: String): Span {
            return GlobalOpenTelemetry
                .getTracer("PokemonMutation")
                .spanBuilder(spanName)
                .startSpan()
        }
    }
}

private fun PokemonEntity.toPokemon() = if (location != null) {
    LegendPokemon(id, name, Pokemon.Color.valueOf(color), location)
} else {
    NormalPokemon(id, name, Pokemon.Color.valueOf(color))
}

private fun Span.ok() = apply { setStatus(StatusCode.OK) }
private fun Span.error(e: Exception? = null) = apply { setStatus(StatusCode.ERROR) }
