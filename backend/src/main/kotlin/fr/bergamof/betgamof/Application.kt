package fr.bergamof.betgamof

import fr.bergamof.betgamof.api.API_AUTH
import fr.bergamof.betgamof.api.Services
import fr.bergamof.betgamof.api.apiRoutes
import fr.bergamof.betgamof.api.configureAuthentication
import fr.bergamof.betgamof.api.configureErrors
import fr.bergamof.betgamof.api.configureSerialization
import fr.bergamof.betgamof.config.AppConfig
import fr.bergamof.betgamof.events.SimulatedEventCatalog
import fr.bergamof.betgamof.persistence.BankrollRepository
import fr.bergamof.betgamof.persistence.BetRepository
import fr.bergamof.betgamof.persistence.DatabaseFactory
import fr.bergamof.betgamof.persistence.MontanteRepository
import fr.bergamof.betgamof.persistence.RuleRepository
import fr.bergamof.betgamof.seed.DemoSeeder
import fr.bergamof.betgamof.service.BankrollService
import fr.bergamof.betgamof.service.BetService
import fr.bergamof.betgamof.service.InsightService
import fr.bergamof.betgamof.service.MontanteService
import fr.bergamof.betgamof.service.PortfolioLoader
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.time.Clock

fun main() {
    val config = AppConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") { module(config) }.start(wait = true)
}

fun Application.module(
    config: AppConfig,
    clock: Clock = Clock.systemUTC(),
) {
    val db = DatabaseFactory.connect(config.databasePath)
    val bankrollRepository = BankrollRepository(db)
    val betRepository = BetRepository(db)
    val montanteRepository = MontanteRepository(db)
    val ruleRepository = RuleRepository(db)
    if (config.seedDemoData) {
        DemoSeeder(bankrollRepository, betRepository, montanteRepository, ruleRepository, clock, config.zone).seedIfEmpty()
    }
    val portfolio = PortfolioLoader(bankrollRepository, betRepository, montanteRepository)
    val services =
        Services(
            bankrolls = BankrollService(portfolio, bankrollRepository, clock),
            bets = BetService(portfolio, betRepository, clock),
            montantes = MontanteService(portfolio, montanteRepository, clock),
            insights = InsightService(portfolio, ruleRepository, clock, config.zone),
            events = SimulatedEventCatalog(clock, config.zone),
        )

    configureSerialization()
    configureAuthentication(config.apiToken)
    configureErrors()
    routing {
        get("/health") { call.respondText("ok") }
        authenticate(API_AUTH) {
            route("/api") { apiRoutes(services) }
        }
    }
}
