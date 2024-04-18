package com.example.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.*

interface Pokemon {
    val name: String
    val color: Color
}

data class NormalPokemon(override val name: String, override val color: Color) : Pokemon
data class LegendPokemon(override val name: String, override val color: Color, val location: String) : Pokemon

enum class Color {
    RED, BLUE, YELLOW
}

class PokemonQuery : Query {
    @GraphQLDescription("Get a Pokemon by name")
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
