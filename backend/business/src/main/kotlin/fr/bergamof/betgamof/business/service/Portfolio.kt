package fr.bergamof.betgamof.business.service

import fr.bergamof.betgamof.business.NotFoundException
import fr.bergamof.betgamof.business.domain.Bankroll
import fr.bergamof.betgamof.business.domain.BankrollLedger
import fr.bergamof.betgamof.business.domain.BankrollPosition
import fr.bergamof.betgamof.business.domain.Bet
import fr.bergamof.betgamof.business.domain.BetTotals
import fr.bergamof.betgamof.business.domain.Montante
import fr.bergamof.betgamof.business.domain.MontanteEngine
import fr.bergamof.betgamof.business.domain.MontanteState
import fr.bergamof.betgamof.business.port.outbound.BankrollRepository
import fr.bergamof.betgamof.business.port.outbound.BetCriteria
import fr.bergamof.betgamof.business.port.outbound.BetRepository
import fr.bergamof.betgamof.business.port.outbound.MontanteRepository

typealias MontanteEntry = Pair<Montante, MontanteState>

/**
 * Reads shared by the services. Each one loads only what its answer depends on: a montante state
 * replays that montante's bets only, a balance adds up aggregated totals instead of every bet.
 */
class Portfolio(
    private val bankrolls: BankrollRepository,
    private val bets: BetRepository,
    private val montantes: MontanteRepository,
) {
    fun bankroll(id: Long): Bankroll = bankrolls.find(id) ?: throw NotFoundException("Bankroll $id introuvable")

    fun bankrollNames(): Map<Long, String> = bankrolls.findAll().associate { it.id to it.settings.name }

    fun bet(id: Long): Bet = bets.find(id) ?: throw NotFoundException("Pari $id introuvable")

    fun montante(id: Long): MontanteEntry =
        montantes.find(id)?.let { states(listOf(it)).single() } ?: throw NotFoundException("Montante $id introuvable")

    /** Montantes with their state, replayed from their own bets (one query for all of them). */
    fun states(montanteList: List<Montante>): List<MontanteEntry> {
        if (montanteList.isEmpty()) return emptyList()
        val betsByMontante = bets.find(BetCriteria(montanteIds = montanteList.map { it.id }.toSet())).groupBy { it.montanteId }
        return montanteList.map { it to MontanteEngine.replay(it, betsByMontante[it.id].orEmpty()) }
    }

    fun allStates(): List<MontanteEntry> = states(montantes.findAll())

    fun position(bankroll: Bankroll): BankrollPosition =
        BankrollLedger.position(
            bankroll,
            directTotals(bankroll.id, bets.directTotalsByBankroll()),
            states(montantes.findByBankroll(bankroll.id)),
        )

    fun positions(): Map<Long, BankrollPosition> {
        val totals = bets.directTotalsByBankroll()
        val states = allStates()
        return bankrolls.findAll().associate { it.id to BankrollLedger.position(it, directTotals(it.id, totals), states) }
    }

    private fun directTotals(
        bankrollId: Long,
        totals: Map<Long, BetTotals>,
    ) = totals[bankrollId] ?: BetTotals.ZERO
}
