package fr.bergamof.betgamof.inbound.rest

import kotlinx.serialization.Serializable

@Serializable
data class ErrorJson(
    val message: String,
)
