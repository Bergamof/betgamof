package fr.bergamof.betgamof

import fr.bergamof.betgamof.adapter.http.UseCases
import fr.bergamof.betgamof.adapter.http.betgamofApi
import fr.bergamof.betgamof.adapter.odds.SimulatedEventCatalog
import fr.bergamof.betgamof.adapter.persistence.SqlitePersistence
import fr.bergamof.betgamof.application.service.BankrollService
import fr.bergamof.betgamof.application.service.BetService
import fr.bergamof.betgamof.application.service.EventService
import fr.bergamof.betgamof.application.service.InsightService
import fr.bergamof.betgamof.application.service.MontanteService
import fr.bergamof.betgamof.application.service.PortfolioLoader
import fr.bergamof.betgamof.config.AppConfig
import fr.bergamof.betgamof.seed.DemoSeeder
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.time.Clock

fun main() {
    val config = AppConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") { module(config) }.start(wait = true)
}

/** Composition root: builds the driven adapters, the use cases on top of them, then the REST API. */
fun Application.module(
    config: AppConfig,
    clock: Clock = Clock.systemUTC(),
) {
    val persistence = SqlitePersistence(config.databasePath)
    if (config.seedDemoData) {
        DemoSeeder(persistence.bankrolls, persistence.bets, persistence.montantes, persistence.rules, clock, config.zone).seedIfEmpty()
    }
    val portfolio = PortfolioLoader(persistence.bankrolls, persistence.bets, persistence.montantes)
    val useCases =
        UseCases(
            bankrolls = BankrollService(portfolio, persistence.bankrolls, clock),
            bets = BetService(portfolio, persistence.bets, clock),
            montantes = MontanteService(portfolio, persistence.montantes, clock),
            insights = InsightService(portfolio, persistence.rules, clock, config.zone),
            events = EventService(SimulatedEventCatalog(clock, config.zone)),
        )
    betgamofApi(useCases, config.apiToken)
}
