package fr.bergamof.betgamof.api

import fr.bergamof.betgamof.events.EventSource
import fr.bergamof.betgamof.service.BankrollService
import fr.bergamof.betgamof.service.BetService
import fr.bergamof.betgamof.service.InsightService
import fr.bergamof.betgamof.service.MontanteService

class Services(
    val bankrolls: BankrollService,
    val bets: BetService,
    val montantes: MontanteService,
    val insights: InsightService,
    val events: EventSource,
)
