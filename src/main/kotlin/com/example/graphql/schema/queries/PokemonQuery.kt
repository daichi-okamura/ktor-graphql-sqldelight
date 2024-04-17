package com.example.graphql.schema.queries

import com.example.graphql.schema.models.Pokemon
import com.expediagroup.graphql.dataloader.KotlinDataLoader
import com.expediagroup.graphql.server.operations.Query
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.runBlocking
import org.dataloader.DataLoaderFactory
import java.util.concurrent.CompletableFuture

data class PokemonSearchParameters(val ids: List<Int>)

object PokemonQuery : Query {
    fun findPokemonById(
        params: PokemonSearchParameters,
        dfe: DataFetchingEnvironment
    ): CompletableFuture<List<Pokemon>> =
        dfe.getDataLoader<Int, Pokemon>(PokemonDataLoader.dataLoaderName)
            .loadMany(params.ids)
}

val PokemonDataLoader = object : KotlinDataLoader<Int, Pokemon?> {
    override val dataLoaderName = "POKEMON_LOADER"
    override fun getDataLoader(graphQLContext: GraphQLContext) =
        DataLoaderFactory.newDataLoader { ids ->
            CompletableFuture.supplyAsync {
                runBlocking { Pokemon.search(ids).toMutableList() }
            }
        }
}
