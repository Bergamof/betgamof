package fr.bergamof.betgamof.inbound.rest

import fr.bergamof.betgamof.business.port.inbound.BankrollUseCases
import fr.bergamof.betgamof.business.port.inbound.BetUseCases
import fr.bergamof.betgamof.business.port.inbound.EventUseCases
import fr.bergamof.betgamof.business.port.inbound.InsightUseCases
import fr.bergamof.betgamof.business.port.inbound.MontanteUseCases

/** The inbound ports this adapter drives. */
class UseCases(
    val bankrolls: BankrollUseCases,
    val bets: BetUseCases,
    val montantes: MontanteUseCases,
    val insights: InsightUseCases,
    val events: EventUseCases,
)
