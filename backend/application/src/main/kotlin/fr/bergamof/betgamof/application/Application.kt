package fr.bergamof.betgamof.application

import fr.bergamof.betgamof.application.config.AppConfig
import fr.bergamof.betgamof.application.seed.DemoSeeder
import fr.bergamof.betgamof.business.service.BankrollService
import fr.bergamof.betgamof.business.service.BetService
import fr.bergamof.betgamof.business.service.EventService
import fr.bergamof.betgamof.business.service.InsightService
import fr.bergamof.betgamof.business.service.MontanteService
import fr.bergamof.betgamof.business.service.Portfolio
import fr.bergamof.betgamof.inbound.rest.UseCases
import fr.bergamof.betgamof.inbound.rest.betgamofApi
import fr.bergamof.betgamof.outbound.odds.SimulatedEventCatalog
import fr.bergamof.betgamof.outbound.persistence.sqlite.SqlitePersistence
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.time.Clock

fun main() {
    val config = AppConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") { module(config) }.start(wait = true)
}

/** Composition root: builds the outbound adapters, the business services on top of them, then the REST API. */
fun Application.module(
    config: AppConfig,
    clock: Clock = Clock.systemUTC(),
) {
    val persistence = SqlitePersistence(config.databasePath)
    if (config.seedDemoData) {
        DemoSeeder(persistence.bankrolls, persistence.bets, persistence.montantes, persistence.rules, clock, config.zone).seedIfEmpty()
    }
    val portfolio = Portfolio(persistence.bankrolls, persistence.bets, persistence.montantes)
    val useCases =
        UseCases(
            bankrolls = BankrollService(portfolio, persistence.bankrolls, persistence.bets, persistence.montantes, clock),
            bets = BetService(portfolio, persistence.bets, persistence.montantes, clock),
            montantes = MontanteService(portfolio, persistence.montantes, clock),
            insights = InsightService(portfolio, persistence.bets, persistence.montantes, persistence.rules, clock, config.zone),
            events = EventService(SimulatedEventCatalog(clock, config.zone)),
        )
    betgamofApi(useCases, config.apiToken)
}
