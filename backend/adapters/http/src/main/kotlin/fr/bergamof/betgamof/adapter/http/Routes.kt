package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.port.inbound.BetFilter
import fr.bergamof.betgamof.application.port.inbound.StatsPeriod
import fr.bergamof.betgamof.domain.BetStatus
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

fun Route.apiRoutes(useCases: UseCases) {
    bankrollRoutes(useCases)
    betRoutes(useCases)
    montanteRoutes(useCases)
    insightRoutes(useCases)
    get("/events") {
        call.respond(useCases.events.upcoming(call.request.queryParameters["q"]).map { it.toJson() })
    }
}

private fun Route.bankrollRoutes(useCases: UseCases) =
    route("/bankrolls") {
        get { call.respond(useCases.bankrolls.list().map { it.toJson() }) }
        post { call.respond(HttpStatusCode.Created, useCases.bankrolls.create(call.receive<BankrollRequest>().toSettings()).toJson()) }
        get("/{id}") { call.respond(useCases.bankrolls.get(call.idParam()).toJson()) }
        put("/{id}") { call.respond(useCases.bankrolls.update(call.idParam(), call.receive<BankrollRequest>().toSettings()).toJson()) }
        delete("/{id}") {
            useCases.bankrolls.delete(call.idParam())
            call.respond(HttpStatusCode.NoContent)
        }
        get("/{id}/kelly") {
            val odds = requireNotNull(call.request.queryParameters["odds"]?.toDoubleOrNull()) { "Paramètre odds invalide" }
            call.respond(useCases.bets.kelly(call.idParam(), odds).toJson())
        }
    }

private fun Route.betRoutes(useCases: UseCases) =
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
            call.respond(useCases.bets.list(filter).map { it.toJson() })
        }
        post { call.respond(HttpStatusCode.Created, useCases.bets.create(call.receive<BetRequest>().toNewBet()).toJson()) }
        get("/{id}") { call.respond(useCases.bets.get(call.idParam()).toJson()) }
        put("/{id}") {
            val request = call.receive<BetUpdateRequest>()
            call.respond(useCases.bets.update(call.idParam(), request.stakeMoney(), request.odds, request.bookmaker).toJson())
        }
        post("/{id}/settlement") {
            call.respond(useCases.bets.settle(call.idParam(), call.receive<SettlementRequest>().toSettlement()).toJson())
        }
        delete("/{id}") {
            useCases.bets.delete(call.idParam())
            call.respond(HttpStatusCode.NoContent)
        }
    }

private fun Route.montanteRoutes(useCases: UseCases) =
    route("/montantes") {
        get { call.respond(useCases.montantes.list().map { it.toJson() }) }
        post { call.respond(HttpStatusCode.Created, useCases.montantes.create(call.receive<MontanteRequest>().toConfig()).toJson()) }
        post("/preview") { call.respond(useCases.montantes.preview(call.receive<MontanteRequest>().toConfig()).toJson()) }
        get("/{id}") { call.respond(useCases.montantes.get(call.idParam()).toJson()) }
        post("/{id}/close") { call.respond(useCases.montantes.close(call.idParam()).toJson()) }
    }

private fun Route.insightRoutes(useCases: UseCases) {
    get("/stats") {
        val period = call.optionalEnum<StatsPeriod>("period") ?: StatsPeriod.ALL
        call.respond(useCases.insights.stats(period, call.optionalLong("bankrollId")).toJson())
    }
    route("/rules") {
        get { call.respond(useCases.insights.rules().map { it.toJson() }) }
        post {
            val request = call.receive<RuleRequest>()
            call.respond(HttpStatusCode.Created, useCases.insights.addRule(request.kind, request.param).map { it.toJson() })
        }
        delete("/{id}") {
            useCases.insights.deleteRule(call.idParam())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
