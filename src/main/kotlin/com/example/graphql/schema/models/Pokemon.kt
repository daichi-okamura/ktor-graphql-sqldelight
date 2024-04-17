package com.example.graphql.schema.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("ポケモン")
@Suppress("unused")
data class Pokemon(
    val id: Int,
    val name: String,
) {
    @Suppress("unused")
    companion object {
        fun search(ids: List<Int>): List<Pokemon> {
            return listOf(
                Pokemon(id = 1, name = "ピカチュウ"),
                Pokemon(id = 2, name = "カビゴン"),
                Pokemon(id = 3, name = "サンダー"),
                Pokemon(id = 4, name = "ミュウツー")
            ).filter { ids.contains(it.id) }
        }
    }
}
