package fr.bergamof.betgamof.adapter.http

import kotlinx.serialization.Serializable

@Serializable
data class ErrorJson(
    val message: String,
)
