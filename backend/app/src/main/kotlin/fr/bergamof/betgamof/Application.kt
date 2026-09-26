package fr.bergamof.betgamof

import fr.bergamof.betgamof.adapter.demo.DemoSeeder
import fr.bergamof.betgamof.adapter.demo.SettableClock
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
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.time.Clock
import java.time.ZoneId

fun main() {
    val config = AppConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") { module(config) }.start(wait = true)
}

/** Composition root: builds the driven adapters, the use cases on top of them, then the driving adapters. */
fun Application.module(
    config: AppConfig,
    clock: Clock = Clock.systemUTC(),
) {
    val persistence = SqlitePersistence(config.databasePath)
    if (config.seedDemoData) {
        seedDemoData(persistence, clock, config.zone)
    }
    betgamofApi(useCases(persistence, clock, config.zone), config.apiToken)
}

private fun useCases(
    persistence: SqlitePersistence,
    clock: Clock,
    zone: ZoneId,
): UseCases {
    val portfolio = PortfolioLoader(persistence.bankrolls, persistence.bets, persistence.montantes)
    return UseCases(
        bankrolls = BankrollService(portfolio, persistence.bankrolls, persistence.transactions, clock),
        bets = BetService(portfolio, persistence.bets, persistence.transactions, clock),
        montantes = MontanteService(portfolio, persistence.montantes, persistence.transactions, clock),
        insights = InsightService(portfolio, persistence.rules, persistence.transactions, clock, zone),
        events = EventService(SimulatedEventCatalog(clock, zone)),
    )
}

/** The seeder drives its own use cases, on the same storage but with a clock it moves to date the history in the past. */
private fun seedDemoData(
    persistence: SqlitePersistence,
    clock: Clock,
    zone: ZoneId,
) {
    val demoClock = SettableClock(clock.instant(), clock.zone)
    val demo = useCases(persistence, demoClock, zone)
    DemoSeeder(demo.bankrolls, demo.bets, demo.montantes, demo.insights, demoClock, zone).seedIfEmpty()
}
