package com.example

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.extensions.plus
import com.expediagroup.graphql.server.ktor.*
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "localhost", module = Application::module).start(wait = true)
}

fun Application.module() {
    install(GraphQL) {
        schema {
            packages = listOf("com.example")
            queries = listOf(PokemonQuery())
            mutations = listOf(PokemonMutation())
            typeHierarchy = mapOf(
                Pokemon::class to listOf(
                    NormalPokemon::class,
                    LegendPokemon::class
                )
            )
        }
        server {
            contextFactory = DefaultContextFactory()
        }
    }
    routing {
        graphQLPostRoute()
        graphiQLRoute()
        graphQLSDLRoute()
    }
}

class PokemonQuery : Query {
    @GraphQLDescription("Get a Pokemon by name")
    @Suppress("unused")
    fun normalPokemon(): NormalPokemon = NormalPokemon("ピカチュウ", Color.YELLOW)

    suspend fun pokemon(): List<Pokemon> = coroutineScope {
        delay(1000)
        listOf(
            NormalPokemon("ピカチュウ", Color.YELLOW),
            LegendPokemon("ミュウツー", Color.BLUE, "カントー地方"),
            LegendPokemon("ゼラオラ", Color.YELLOW, "アローラ地方")
        )
    }

    fun date(env: DataFetchingEnvironment): String {
        val locale: Locale = env.graphQlContext.get("locale")
        val dataFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale)
        return dataFormat.format(Date())
    }
}

class PokemonMutation : Mutation {
    fun addPokemon(name: String, color: Color, location: String? = null): Pokemon = location?.let {
        LegendPokemon(name, color, it)
    } ?: NormalPokemon(name, color)
}

interface Pokemon {
    val name: String
    val color: Color
}

enum class Color {
    RED, BLUE, YELLOW
}

data class NormalPokemon(override val name: String, override val color: Color) : Pokemon
data class LegendPokemon(override val name: String, override val color: Color, val location: String) : Pokemon

class DefaultContextFactory : DefaultKtorGraphQLContextFactory() {
    override suspend fun generateContext(request: ApplicationRequest): GraphQLContext {
        return super.generateContext(request).plus(
            mapOf("locale" to Locale.forLanguageTag(request.headers["Accept-Language"] ?: "ja-JP"))
        )
    }
}