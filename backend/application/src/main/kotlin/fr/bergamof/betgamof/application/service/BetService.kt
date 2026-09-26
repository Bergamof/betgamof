package fr.bergamof.betgamof.application.service

import fr.bergamof.betgamof.application.model.BetView
import fr.bergamof.betgamof.application.port.inbound.AmendBetCommand
import fr.bergamof.betgamof.application.port.inbound.BetFilter
import fr.bergamof.betgamof.application.port.inbound.BetUseCases
import fr.bergamof.betgamof.application.port.inbound.PlaceBetCommand
import fr.bergamof.betgamof.application.port.inbound.SettleBetCommand
import fr.bergamof.betgamof.application.port.outbound.BetRepository
import fr.bergamof.betgamof.application.port.outbound.TransactionRunner
import fr.bergamof.betgamof.domain.Bet
import fr.bergamof.betgamof.domain.BetId
import fr.bergamof.betgamof.domain.NewBet
import fr.bergamof.betgamof.domain.Selection
import fr.bergamof.betgamof.domain.Settlement
import java.time.Clock
import java.time.Duration

class BetService(
    private val portfolio: PortfolioLoader,
    private val repository: BetRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) : BetUseCases {
    override fun list(filter: BetFilter): List<BetView> =
        transactions.inTransaction {
            val snapshot = portfolio.load()
            val since = filter.lastDays?.let { clock.instant() - Duration.ofDays(it) }
            snapshot.bets
                .asSequence()
                .filter { filter.status == null || it.status == filter.status }
                .filter { filter.bankrollId == null || it.bankrollId == filter.bankrollId }
                .filter { filter.sport == null || it.sport.equals(filter.sport, ignoreCase = true) }
                .filter { filter.bookmaker == null || it.bookmaker.equals(filter.bookmaker, ignoreCase = true) }
                .filter { since == null || it.startsAt >= since }
                .filter { filter.query.isNullOrBlank() || matches(it, filter.query) }
                .sortedWith(compareByDescending<Bet> { it.startsAt }.thenByDescending { it.id.value })
                .map { it.toView(snapshot) }
                .toList()
        }

    override fun get(id: BetId): BetView =
        transactions.inTransaction {
            val snapshot = portfolio.load()
            snapshot.requireBet(id).toView(snapshot)
        }

    override fun place(command: PlaceBetCommand): BetView =
        transactions.inTransaction {
            val snapshot = portfolio.load()
            val newBet = command.toNewBet()
            val now = clock.instant()
            val bet =
                if (command.montanteId != null) {
                    snapshot.requireMontante(command.montanteId).placePalier(newBet, now)
                } else {
                    snapshot.requireBankroll(command.bankrollId)
                    Bet.place(newBet, now)
                }
            get(repository.add(bet).id)
        }

    override fun amend(
        id: BetId,
        command: AmendBetCommand,
    ): BetView =
        transactions.inTransaction {
            val bet = repository.findById(id) ?: throw betNotFound(id)
            repository.save(bet.amend(command.stake, command.odds, command.bookmaker))
            get(id)
        }

    override fun settle(
        id: BetId,
        command: SettleBetCommand,
    ): BetView =
        transactions.inTransaction {
            val bet = repository.findById(id) ?: throw betNotFound(id)
            repository.save(bet.settle(Settlement(command.status, command.cashout), clock.instant()))
            get(id)
        }

    override fun delete(id: BetId) =
        transactions.inTransaction {
            val bet = repository.findById(id) ?: throw betNotFound(id)
            bet.ensureDeletable()
            repository.remove(id)
        }

    private fun PlaceBetCommand.toNewBet() =
        NewBet.of(
            bankrollId = bankrollId,
            type = type,
            bookmaker = bookmaker,
            stake = stake,
            odds = odds,
            startsAt = startsAt,
            selections = selections.map { Selection.of(it.eventId, it.eventName, it.sport, it.competition, it.market, it.pick, it.odds) },
        )

    private fun matches(
        bet: Bet,
        query: String,
    ): Boolean {
        val haystack =
            (bet.selections.flatMap { listOf(it.eventName, it.pick, it.competition, it.sport) } + bet.bookmaker).joinToString(" ")
        return haystack.contains(query.trim(), ignoreCase = true)
    }
}
