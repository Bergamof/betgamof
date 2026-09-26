package fr.bergamof.betgamof.application

import fr.bergamof.betgamof.application.port.outbound.BankrollRepository
import fr.bergamof.betgamof.application.port.outbound.BetRepository
import fr.bergamof.betgamof.application.port.outbound.MontanteRepository
import fr.bergamof.betgamof.application.port.outbound.RuleRepository
import fr.bergamof.betgamof.domain.Bankroll
import fr.bergamof.betgamof.domain.BankrollSettings
import fr.bergamof.betgamof.domain.Bet
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.DisciplineRule
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.Montante
import fr.bergamof.betgamof.domain.MontanteConfig
import fr.bergamof.betgamof.domain.NewBet
import fr.bergamof.betgamof.domain.RuleKind
import fr.bergamof.betgamof.domain.Settlement
import java.time.Instant

// In-memory adapters for the outbound ports: use cases are tested without a database.

class InMemoryBankrollRepository : BankrollRepository {
    private val rows = linkedMapOf<Long, Bankroll>()

    override fun findAll() = rows.values.toList()

    override fun find(id: Long) = rows[id]

    override fun create(
        settings: BankrollSettings,
        now: Instant,
    ): Long {
        val id = rows.size + 1L
        rows[id] = Bankroll(id, settings, now)
        return id
    }

    override fun update(
        id: Long,
        settings: BankrollSettings,
    ) = rows.computeIfPresent(id) { _, bankroll -> bankroll.copy(settings = settings) } != null

    override fun delete(id: Long) = rows.remove(id) != null
}

class InMemoryBetRepository : BetRepository {
    private val rows = linkedMapOf<Long, Bet>()

    override fun findAll() = rows.values.toList()

    override fun find(id: Long) = rows[id]

    override fun create(
        bet: NewBet,
        placedAt: Instant,
    ): Long {
        val id = rows.size + 1L
        rows[id] =
            Bet(
                id = id,
                bankrollId = bet.bankrollId,
                montanteId = bet.montanteId,
                type = bet.type,
                bookmaker = bet.bookmaker,
                stake = bet.stake,
                odds = bet.odds,
                status = BetStatus.OPEN,
                cashout = null,
                selections = bet.selections,
                placedAt = placedAt,
                startsAt = bet.startsAt,
                settledAt = null,
            )
        return id
    }

    override fun updateStakeAndOdds(
        id: Long,
        stake: Money,
        odds: Double,
        bookmaker: String,
    ) = rows.computeIfPresent(id) { _, bet -> bet.copy(stake = stake, odds = odds, bookmaker = bookmaker) } != null

    override fun settle(
        id: Long,
        settlement: Settlement,
        at: Instant,
    ) = rows.computeIfPresent(id) { _, bet -> bet.copy(status = settlement.status, cashout = settlement.cashout, settledAt = at) } != null

    override fun delete(id: Long) = rows.remove(id) != null
}

class InMemoryMontanteRepository : MontanteRepository {
    private val rows = linkedMapOf<Long, Montante>()

    override fun findAll() = rows.values.toList()

    override fun find(id: Long) = rows[id]

    override fun create(
        config: MontanteConfig,
        now: Instant,
    ): Long {
        val id = rows.size + 1L
        rows[id] = Montante(id, config, now, null)
        return id
    }

    override fun close(
        id: Long,
        at: Instant,
    ) = rows.computeIfPresent(id) { _, montante -> montante.copy(closedAt = at) } != null
}

class InMemoryRuleRepository : RuleRepository {
    private val rows = linkedMapOf<Long, DisciplineRule>()

    override fun findAll() = rows.values.toList()

    override fun create(
        kind: RuleKind,
        param: Int,
    ): Long {
        val id = rows.size + 1L
        rows[id] = DisciplineRule(id, kind, param)
        return id
    }

    override fun delete(id: Long) = rows.remove(id) != null
}
