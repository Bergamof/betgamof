package fr.bergamof.betgamof.application.service

import fr.bergamof.betgamof.application.NotFoundException
import fr.bergamof.betgamof.application.port.outbound.BankrollRepository
import fr.bergamof.betgamof.application.port.outbound.BetRepository
import fr.bergamof.betgamof.application.port.outbound.MontanteRepository
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.BetId
import fr.bergamof.betgamof.domain.MontanteId
import fr.bergamof.betgamof.domain.Portfolio

/** Loads the whole portfolio: balances and montante states are derived from the full history. */
class PortfolioLoader(
    private val bankrolls: BankrollRepository,
    private val bets: BetRepository,
    private val montantes: MontanteRepository,
) {
    fun load() = Portfolio(bankrolls.findAll(), bets.findAll(), montantes.findAll())
}

internal fun Portfolio.requireBankroll(id: BankrollId) = bankroll(id) ?: throw bankrollNotFound(id)

internal fun Portfolio.requireMontante(id: MontanteId) = montante(id) ?: throw NotFoundException("Montante ${id.value} introuvable")

internal fun Portfolio.requireBet(id: BetId) = bet(id) ?: throw betNotFound(id)

internal fun bankrollNotFound(id: BankrollId) = NotFoundException("Bankroll ${id.value} introuvable")

internal fun betNotFound(id: BetId) = NotFoundException("Pari ${id.value} introuvable")
