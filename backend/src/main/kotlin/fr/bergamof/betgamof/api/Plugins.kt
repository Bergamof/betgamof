package fr.bergamof.betgamof.api

import fr.bergamof.betgamof.service.ConflictException
import fr.bergamof.betgamof.service.NotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.security.MessageDigest

const val API_AUTH = "api-token"

private val logger = LoggerFactory.getLogger("fr.bergamof.betgamof.api")

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = true
            },
        )
    }
}

fun Application.configureAuthentication(apiToken: String) {
    val expected = apiToken.toByteArray()
    install(Authentication) {
        bearer(API_AUTH) {
            authenticate { credential ->
                // Constant-time comparison so the token cannot be guessed byte by byte.
                if (MessageDigest.isEqual(credential.token.toByteArray(), expected)) UserIdPrincipal("owner") else null
            }
        }
    }
}

fun Application.configureErrors() {
    install(CallLogging)
    install(StatusPages) {
        exception<NotFoundException> { call, cause -> call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message.orEmpty())) }
        exception<ConflictException> { call, cause -> call.respond(HttpStatusCode.Conflict, ErrorResponse(cause.message.orEmpty())) }
        exception<IllegalArgumentException> {
            call,
            cause,
            ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message.orEmpty()))
        }
        exception<BadRequestException> { call, cause ->
            val message = generateSequence(cause as Throwable) { it.cause }.last().message ?: "Requête invalide"
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(message))
        }
        exception<Throwable> { call, cause ->
            logger.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur interne"))
        }
    }
}
