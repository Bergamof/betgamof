package fr.bergamof.betgamof.business.service

import fr.bergamof.betgamof.business.ConflictException
import fr.bergamof.betgamof.business.domain.Bet
import fr.bergamof.betgamof.business.domain.BetChange
import fr.bergamof.betgamof.business.domain.KellyAdvisor
import fr.bergamof.betgamof.business.domain.NewBet
import fr.bergamof.betgamof.business.domain.OddsRange
import fr.bergamof.betgamof.business.domain.Period
import fr.bergamof.betgamof.business.domain.Settlement
import fr.bergamof.betgamof.business.model.BetView
import fr.bergamof.betgamof.business.model.KellyView
import fr.bergamof.betgamof.business.port.inbound.BetFilter
import fr.bergamof.betgamof.business.port.inbound.BetUseCases
import fr.bergamof.betgamof.business.port.outbound.BetCriteria
import fr.bergamof.betgamof.business.port.outbound.BetOrder
import fr.bergamof.betgamof.business.port.outbound.BetRepository
import fr.bergamof.betgamof.business.port.outbound.MontanteRepository
import java.time.Clock
import java.time.Duration

class BetService(
    private val portfolio: Portfolio,
    private val repository: BetRepository,
    private val montantes: MontanteRepository,
    private val clock: Clock,
) : BetUseCases {
    override fun list(filter: BetFilter): List<BetView> {
        val criteria =
            BetCriteria(
                bankrollId = filter.bankrollId,
                status = filter.status,
                sport = filter.sport,
                bookmaker = filter.bookmaker,
                text = filter.query?.takeIf { it.isNotBlank() }?.trim(),
                starts = filter.lastDays?.let { Period.last(Duration.ofDays(it), clock.instant()) },
                order = BetOrder.LATEST_START,
                page = filter.page,
            )
        return views(repository.find(criteria))
    }

    override fun get(id: Long): BetView = views(listOf(portfolio.bet(id))).single()

    override fun create(request: NewBet): BetView {
        portfolio.bankroll(request.bankrollId)
        val bet = request.montanteId?.let { attachToMontante(request, it) } ?: request
        return get(repository.create(bet, clock.instant()))
    }

    override fun update(
        id: Long,
        change: BetChange,
    ): BetView {
        val bet = portfolio.bet(id)
        if (!bet.isOpen) throw ConflictException("Seul un pari en cours peut être modifié")
        if (bet.montanteId != null && change.stake != bet.stake) throw ConflictException("La mise d'un palier est fixée par la montante")
        repository.update(id, change)
        return get(id)
    }

    override fun settle(
        id: Long,
        settlement: Settlement,
    ): BetView {
        val bet = portfolio.bet(id)
        if (!bet.isOpen) throw ConflictException("Ce pari est déjà réglé")
        repository.settle(id, settlement, clock.instant())
        return get(id)
    }

    override fun delete(id: Long) {
        val bet = portfolio.bet(id)
        if (bet.montanteId != null && !bet.isOpen) {
            throw ConflictException("Un palier réglé fait partie de l'historique de la montante et ne peut pas être supprimé")
        }
        repository.delete(id)
    }

    override fun kelly(
        bankrollId: Long,
        odds: Double,
    ): KellyView {
        require(odds > 1.0) { "La cote doit être supérieure à 1" }
        val bankroll = portfolio.bankroll(bankrollId)
        val sameRange = repository.find(BetCriteria(decidedOnly = true, oddsRange = OddsRange.of(odds)))
        val advice = KellyAdvisor.advise(odds, portfolio.position(bankroll).balance, bankroll.settings.kellyFraction, sameRange)
        return KellyView(advice.stake, advice.bankrollShare, advice.probability)
    }

    /** Views of [bets], replaying only the montantes they belong to. */
    private fun views(bets: List<Bet>): List<BetView> {
        val montanteIds = bets.mapNotNull { it.montanteId }.toSet()
        val entries = portfolio.states(montantes.findAll().filter { it.id in montanteIds }).associateBy { it.first.id }
        val bankrollNames = portfolio.bankrollNames()
        return bets.map { it.toView(bankrollNames.getValue(it.bankrollId), it.montanteId?.let(entries::get)) }
    }

    /** A montante bet always stakes the whole current capital, on the montante's bankroll. */
    private fun attachToMontante(
        request: NewBet,
        montanteId: Long,
    ): NewBet {
        val (montante, state) = portfolio.montante(montanteId)
        if (!state.isActive) throw ConflictException("La montante « ${montante.config.name} » est terminée")
        if (state.openBet != null) throw ConflictException("Le palier ${state.currentPalierNumber} a déjà un pari en cours")
        return request.copy(bankrollId = montante.config.bankrollId, stake = state.capital)
    }
}
