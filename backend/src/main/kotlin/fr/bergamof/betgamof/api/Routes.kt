package fr.bergamof.betgamof.api

import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.service.BetFilter
import fr.bergamof.betgamof.service.StatsPeriod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

private fun ApplicationCall.idParam(): Long = requireNotNull(parameters["id"]?.toLongOrNull()) { "Identifiant invalide" }

private fun ApplicationCall.optionalLong(name: String): Long? =
    request.queryParameters[name]?.let { requireNotNull(it.toLongOrNull()) { "Paramètre $name invalide" } }

private inline fun <reified T : Enum<T>> ApplicationCall.optionalEnum(name: String): T? =
    request.queryParameters[name]?.let { value ->
        requireNotNull(enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) }) { "Paramètre $name invalide" }
    }

fun Route.apiRoutes(services: Services) {
    bankrollRoutes(services)
    betRoutes(services)
    montanteRoutes(services)
    insightRoutes(services)
    get("/events") {
        val query =
            call.request.queryParameters["q"]
                ?.trim()
                .orEmpty()
        call.respond(services.events.upcoming().filter { query.isEmpty() || it.name.contains(query, ignoreCase = true) })
    }
}

private fun Route.bankrollRoutes(services: Services) =
    route("/bankrolls") {
        get { call.respond(services.bankrolls.list()) }
        post { call.respond(HttpStatusCode.Created, services.bankrolls.create(call.receive<BankrollRequest>().toSettings())) }
        get("/{id}") { call.respond(services.bankrolls.get(call.idParam())) }
        put("/{id}") { call.respond(services.bankrolls.update(call.idParam(), call.receive<BankrollRequest>().toSettings())) }
        delete("/{id}") {
            services.bankrolls.delete(call.idParam())
            call.respond(HttpStatusCode.NoContent)
        }
        get("/{id}/kelly") {
            val odds = requireNotNull(call.request.queryParameters["odds"]?.toDoubleOrNull()) { "Paramètre odds invalide" }
            call.respond(services.bets.kelly(call.idParam(), odds))
        }
    }

private fun Route.betRoutes(services: Services) =
    route("/bets") {
        get {
            val params = call.request.queryParameters
            val filter =
                BetFilter(
                    status = call.optionalEnum<BetStatus>("status"),
                    bankrollId = call.optionalLong("bankrollId"),
                    sport = params["sport"]?.takeIf { it.isNotBlank() },
                    bookmaker = params["bookmaker"]?.takeIf { it.isNotBlank() },
                    lastDays = call.optionalLong("lastDays"),
                    query = params["q"],
                )
            call.respond(services.bets.list(filter))
        }
        post { call.respond(HttpStatusCode.Created, services.bets.create(call.receive<BetRequest>().toNewBet())) }
        get("/{id}") { call.respond(services.bets.get(call.idParam())) }
        put("/{id}") {
            val request = call.receive<BetUpdateRequest>()
            call.respond(services.bets.update(call.idParam(), request.stake, request.odds, request.bookmaker))
        }
        post("/{id}/settlement") { call.respond(services.bets.settle(call.idParam(), call.receive<SettlementRequest>().toSettlement())) }
        delete("/{id}") {
            services.bets.delete(call.idParam())
            call.respond(HttpStatusCode.NoContent)
        }
    }

private fun Route.montanteRoutes(services: Services) =
    route("/montantes") {
        get { call.respond(services.montantes.list()) }
        post { call.respond(HttpStatusCode.Created, services.montantes.create(call.receive<MontanteRequest>().toConfig())) }
        post("/preview") { call.respond(services.montantes.preview(call.receive<MontanteRequest>().toConfig())) }
        get("/{id}") { call.respond(services.montantes.get(call.idParam())) }
        post("/{id}/close") { call.respond(services.montantes.close(call.idParam())) }
    }

private fun Route.insightRoutes(services: Services) {
    get("/stats") {
        val period = call.optionalEnum<StatsPeriod>("period") ?: StatsPeriod.ALL
        call.respond(services.insights.stats(period, call.optionalLong("bankrollId")))
    }
    route("/rules") {
        get { call.respond(services.insights.rules()) }
        post {
            val request = call.receive<RuleRequest>()
            call.respond(HttpStatusCode.Created, services.insights.addRule(request.kind, request.param))
        }
        delete("/{id}") {
            services.insights.deleteRule(call.idParam())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
