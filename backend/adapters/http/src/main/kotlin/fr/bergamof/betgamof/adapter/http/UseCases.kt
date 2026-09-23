package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.port.inbound.BankrollUseCases
import fr.bergamof.betgamof.application.port.inbound.BetUseCases
import fr.bergamof.betgamof.application.port.inbound.EventUseCases
import fr.bergamof.betgamof.application.port.inbound.InsightUseCases
import fr.bergamof.betgamof.application.port.inbound.MontanteUseCases

/** The inbound ports this adapter drives. */
class UseCases(
    val bankrolls: BankrollUseCases,
    val bets: BetUseCases,
    val montantes: MontanteUseCases,
    val insights: InsightUseCases,
    val events: EventUseCases,
)
