package com.example

import com.expediagroup.graphql.generator.extensions.plus
import com.expediagroup.graphql.server.ktor.*
import graphql.GraphQLContext
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import java.util.*

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    DatabaseSingleton.init(environment.config)

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

class DefaultContextFactory : DefaultKtorGraphQLContextFactory() {
    override suspend fun generateContext(request: ApplicationRequest): GraphQLContext {
        return super.generateContext(request).plus(
            mapOf("locale" to Locale.forLanguageTag(request.headers["Accept-Language"] ?: "ja-JP"))
        )
    }
}
