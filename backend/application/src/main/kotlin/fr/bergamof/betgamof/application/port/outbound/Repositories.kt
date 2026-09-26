package fr.bergamof.betgamof.application.port.outbound

import fr.bergamof.betgamof.domain.Bankroll
import fr.bergamof.betgamof.domain.BankrollSettings
import fr.bergamof.betgamof.domain.Bet
import fr.bergamof.betgamof.domain.DisciplineRule
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.Montante
import fr.bergamof.betgamof.domain.MontanteConfig
import fr.bergamof.betgamof.domain.NewBet
import fr.bergamof.betgamof.domain.RuleKind
import fr.bergamof.betgamof.domain.Settlement
import java.time.Instant

// Driven ports: what the application needs from storage. Implemented by adapters/persistence.

interface BankrollRepository {
    fun findAll(): List<Bankroll>

    fun find(id: Long): Bankroll?

    fun create(
        settings: BankrollSettings,
        now: Instant,
    ): Long

    fun update(
        id: Long,
        settings: BankrollSettings,
    ): Boolean

    fun delete(id: Long): Boolean
}

interface BetRepository {
    fun findAll(): List<Bet>

    fun find(id: Long): Bet?

    fun create(
        bet: NewBet,
        placedAt: Instant,
    ): Long

    fun updateStakeAndOdds(
        id: Long,
        stake: Money,
        odds: Double,
        bookmaker: String,
    ): Boolean

    fun settle(
        id: Long,
        settlement: Settlement,
        at: Instant,
    ): Boolean

    fun delete(id: Long): Boolean
}

interface MontanteRepository {
    fun findAll(): List<Montante>

    fun find(id: Long): Montante?

    fun create(
        config: MontanteConfig,
        now: Instant,
    ): Long

    fun close(
        id: Long,
        at: Instant,
    ): Boolean
}

interface RuleRepository {
    fun findAll(): List<DisciplineRule>

    fun create(
        kind: RuleKind,
        param: Int,
    ): Long

    fun delete(id: Long): Boolean
}
