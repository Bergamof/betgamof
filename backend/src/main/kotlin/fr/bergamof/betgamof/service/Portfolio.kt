package fr.bergamof.betgamof.service

import fr.bergamof.betgamof.domain.Bankroll
import fr.bergamof.betgamof.domain.BankrollLedger
import fr.bergamof.betgamof.domain.BankrollPosition
import fr.bergamof.betgamof.domain.Bet
import fr.bergamof.betgamof.domain.Montante
import fr.bergamof.betgamof.domain.MontanteEngine
import fr.bergamof.betgamof.domain.MontanteState
import fr.bergamof.betgamof.persistence.BankrollRepository
import fr.bergamof.betgamof.persistence.BetRepository
import fr.bergamof.betgamof.persistence.MontanteRepository

/** Everything the app knows at a given moment, with derived montante states and balances. */
class PortfolioSnapshot(
    val bankrolls: List<Bankroll>,
    val bets: List<Bet>,
    montantes: List<Montante>,
) {
    val montantes: List<Pair<Montante, MontanteState>> =
        montantes.map { montante -> montante to MontanteEngine.replay(montante, bets.filter { it.montanteId == montante.id }) }

    val positions: Map<Long, BankrollPosition> by lazy {
        bankrolls.associate { it.id to BankrollLedger.position(it, bets, this.montantes) }
    }

    fun bankroll(id: Long) = bankrolls.firstOrNull { it.id == id } ?: throw NotFoundException("Bankroll $id introuvable")

    fun montante(id: Long) =
        montantes.firstOrNull { (montante, _) -> montante.id == id } ?: throw NotFoundException("Montante $id introuvable")

    fun bet(id: Long) = bets.firstOrNull { it.id == id } ?: throw NotFoundException("Pari $id introuvable")

    fun position(bankrollId: Long) = positions.getValue(bankrollId)
}

class PortfolioLoader(
    private val bankrolls: BankrollRepository,
    private val bets: BetRepository,
    private val montantes: MontanteRepository,
) {
    fun load() = PortfolioSnapshot(bankrolls.findAll(), bets.findAll(), montantes.findAll())
}
