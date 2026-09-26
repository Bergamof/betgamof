package fr.bergamof.betgamof.application.service

import fr.bergamof.betgamof.application.ConflictException
import fr.bergamof.betgamof.application.model.BetView
import fr.bergamof.betgamof.application.model.KellyView
import fr.bergamof.betgamof.application.port.inbound.BetFilter
import fr.bergamof.betgamof.application.port.inbound.BetUseCases
import fr.bergamof.betgamof.application.port.outbound.BetRepository
import fr.bergamof.betgamof.domain.Bet
import fr.bergamof.betgamof.domain.KellyAdvisor
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.NewBet
import fr.bergamof.betgamof.domain.Settlement
import java.time.Clock
import java.time.Duration

class BetService(
    private val portfolio: PortfolioLoader,
    private val repository: BetRepository,
    private val clock: Clock,
) : BetUseCases {
    override fun list(filter: BetFilter): List<BetView> {
        val snapshot = portfolio.load()
        val since = filter.lastDays?.let { clock.instant() - Duration.ofDays(it) }
        return snapshot.bets
            .asSequence()
            .filter { filter.status == null || it.status == filter.status }
            .filter { filter.bankrollId == null || it.bankrollId == filter.bankrollId }
            .filter { filter.sport == null || it.sport.equals(filter.sport, ignoreCase = true) }
            .filter { filter.bookmaker == null || it.bookmaker.equals(filter.bookmaker, ignoreCase = true) }
            .filter { since == null || it.startsAt >= since }
            .filter { filter.query.isNullOrBlank() || matches(it, filter.query) }
            .sortedWith(compareByDescending<Bet> { it.startsAt }.thenByDescending { it.id })
            .map { it.toView(snapshot) }
            .toList()
    }

    override fun get(id: Long): BetView {
        val snapshot = portfolio.load()
        return snapshot.bet(id).toView(snapshot)
    }

    override fun create(request: NewBet): BetView {
        val snapshot = portfolio.load()
        snapshot.bankroll(request.bankrollId)
        val bet = request.montanteId?.let { attachToMontante(request, it, snapshot) } ?: request
        return get(repository.create(bet, clock.instant()))
    }

    override fun update(
        id: Long,
        stake: Money,
        odds: Double,
        bookmaker: String,
    ): BetView {
        require(stake.isPositive) { "La mise doit être positive" }
        require(odds > 1.0) { "La cote doit être supérieure à 1" }
        require(bookmaker.isNotBlank()) { "Le bookmaker est obligatoire" }
        val bet = portfolio.load().bet(id)
        if (!bet.isOpen) throw ConflictException("Seul un pari en cours peut être modifié")
        if (bet.montanteId != null && stake != bet.stake) throw ConflictException("La mise d'un palier est fixée par la montante")
        repository.updateStakeAndOdds(id, stake, odds, bookmaker)
        return get(id)
    }

    override fun settle(
        id: Long,
        settlement: Settlement,
    ): BetView {
        val bet = portfolio.load().bet(id)
        if (!bet.isOpen) throw ConflictException("Ce pari est déjà réglé")
        repository.settle(id, settlement, clock.instant())
        return get(id)
    }

    override fun delete(id: Long) {
        val bet = portfolio.load().bet(id)
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
        val snapshot = portfolio.load()
        val bankroll = snapshot.bankroll(bankrollId)
        val advice = KellyAdvisor.advise(odds, snapshot.position(bankrollId).balance, bankroll.settings.kellyFraction, snapshot.bets)
        return KellyView(advice.stake, advice.bankrollShare, advice.probability)
    }

    /** A montante bet always stakes the whole current capital, on the montante's bankroll. */
    private fun attachToMontante(
        request: NewBet,
        montanteId: Long,
        snapshot: PortfolioSnapshot,
    ): NewBet {
        val (montante, state) = snapshot.montante(montanteId)
        if (!state.isActive) throw ConflictException("La montante « ${montante.config.name} » est terminée")
        if (state.openBet != null) throw ConflictException("Le palier ${state.currentPalierNumber} a déjà un pari en cours")
        return request.copy(bankrollId = montante.config.bankrollId, stake = state.capital)
    }

    private fun matches(
        bet: Bet,
        query: String,
    ): Boolean {
        val haystack =
            (
                bet.selections.flatMap {
                    listOf(
                        it.eventName,
                        it.pick,
                        it.competition,
                        it.sport,
                    )
                } + bet.bookmaker
            ).joinToString(" ")
        return haystack.contains(query.trim(), ignoreCase = true)
    }
}
