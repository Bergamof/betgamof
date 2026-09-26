package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.port.inbound.BetFilter
import fr.bergamof.betgamof.application.port.inbound.StatsPeriod
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.BetId
import fr.bergamof.betgamof.domain.MontanteId
import fr.bergamof.betgamof.domain.RuleId
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

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
        post { call.respond(HttpStatusCode.Created, useCases.bankrolls.create(call.receive<BankrollRequest>().toCommand()).toJson()) }
        get("/{id}") { call.respond(useCases.bankrolls.get(BankrollId(call.idParam())).toJson()) }
        put("/{id}") {
            val id = BankrollId(call.idParam())
            call.respond(useCases.bankrolls.update(id, call.receive<BankrollRequest>().toCommand()).toJson())
        }
        delete("/{id}") {
            useCases.bankrolls.delete(BankrollId(call.idParam()))
            call.respond(HttpStatusCode.NoContent)
        }
        get("/{id}/kelly") {
            val id = BankrollId(call.idParam())
            call.respond(useCases.bankrolls.adviseStake(id, call.requiredDouble("odds")).toJson())
        }
    }

private fun Route.betRoutes(useCases: UseCases) =
    route("/bets") {
        get {
            val params = call.request.queryParameters
            val filter =
                BetFilter(
                    status = call.optionalEnum<BetStatusJson>("status")?.toDomain(),
                    bankrollId = call.optionalLong("bankrollId")?.let(::BankrollId),
                    sport = params["sport"]?.takeIf { it.isNotBlank() },
                    bookmaker = params["bookmaker"]?.takeIf { it.isNotBlank() },
                    lastDays = call.optionalLong("lastDays"),
                    query = params["q"],
                )
            call.respond(useCases.bets.list(filter).map { it.toJson() })
        }
        post { call.respond(HttpStatusCode.Created, useCases.bets.place(call.receive<BetRequest>().toCommand()).toJson()) }
        get("/{id}") { call.respond(useCases.bets.get(BetId(call.idParam())).toJson()) }
        put("/{id}") {
            val id = BetId(call.idParam())
            call.respond(useCases.bets.amend(id, call.receive<BetUpdateRequest>().toCommand()).toJson())
        }
        post("/{id}/settlement") {
            val id = BetId(call.idParam())
            call.respond(useCases.bets.settle(id, call.receive<SettlementRequest>().toCommand()).toJson())
        }
        delete("/{id}") {
            useCases.bets.delete(BetId(call.idParam()))
            call.respond(HttpStatusCode.NoContent)
        }
    }

private fun Route.montanteRoutes(useCases: UseCases) =
    route("/montantes") {
        get { call.respond(useCases.montantes.list().map { it.toJson() }) }
        post { call.respond(HttpStatusCode.Created, useCases.montantes.start(call.receive<MontanteRequest>().toCommand()).toJson()) }
        post("/preview") { call.respond(useCases.montantes.preview(call.receive<MontanteRequest>().toCommand()).toJson()) }
        get("/{id}") { call.respond(useCases.montantes.get(MontanteId(call.idParam())).toJson()) }
        post("/{id}/close") { call.respond(useCases.montantes.close(MontanteId(call.idParam())).toJson()) }
    }

private fun Route.insightRoutes(useCases: UseCases) {
    get("/stats") {
        val period = call.optionalEnum<StatsPeriodJson>("period")?.toDomain() ?: StatsPeriod.ALL
        call.respond(useCases.insights.stats(period, call.optionalLong("bankrollId")?.let(::BankrollId)).toJson())
    }
    route("/rules") {
        get { call.respond(useCases.insights.rules().map { it.toJson() }) }
        post {
            call.respond(HttpStatusCode.Created, useCases.insights.addRule(call.receive<RuleRequest>().toCommand()).map { it.toJson() })
        }
        delete("/{id}") {
            useCases.insights.deleteRule(RuleId(call.idParam()))
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
