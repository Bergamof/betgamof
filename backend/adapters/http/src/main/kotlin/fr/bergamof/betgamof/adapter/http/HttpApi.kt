package fr.bergamof.betgamof.adapter.http

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * Installs the REST API on a Ktor application: JSON, bearer-token authentication, error mapping,
 * a public `/health` probe and the authenticated `/api/...` routes.
 */
fun Application.betgamofApi(
    useCases: UseCases,
    apiToken: String,
) {
    configureSerialization()
    configureAuthentication(apiToken)
    configureErrors()
    routing {
        get("/health") { call.respondText("ok") }
        authenticate(API_AUTH) {
            route("/api") { apiRoutes(useCases) }
        }
    }
}
