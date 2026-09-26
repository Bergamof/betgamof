package fr.bergamof.betgamof.adapter.http

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.serializer

// Path and query parameter parsing. A malformed parameter is a client error (400), never a bug (500),
// so failures are reported as BadRequestException rather than with require().

internal fun ApplicationCall.idParam(): Long = parameters["id"]?.toLongOrNull() ?: throw BadRequestException("Identifiant invalide")

internal fun ApplicationCall.optionalLong(name: String): Long? =
    request.queryParameters[name]?.let { it.toLongOrNull() ?: throw BadRequestException("Paramètre $name invalide") }

internal fun ApplicationCall.requiredDouble(name: String): Double =
    request.queryParameters[name]?.toDoubleOrNull() ?: throw BadRequestException("Paramètre $name invalide")

/** Parses a contract enum from its JSON spelling, ignoring case. */
internal inline fun <reified T : Enum<T>> ApplicationCall.optionalEnum(name: String): T? =
    request.queryParameters[name]?.let { value ->
        val descriptor = serializer<T>().descriptor
        val index = (0 until descriptor.elementsCount).firstOrNull { descriptor.getElementName(it).equals(value, ignoreCase = true) }
        index?.let { enumValues<T>()[it] } ?: throw BadRequestException("Paramètre $name invalide")
    }
