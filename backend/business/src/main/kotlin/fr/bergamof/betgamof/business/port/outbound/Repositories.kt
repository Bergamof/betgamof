package fr.bergamof.betgamof.business.port.outbound

import fr.bergamof.betgamof.business.domain.Bankroll
import fr.bergamof.betgamof.business.domain.BankrollSettings
import fr.bergamof.betgamof.business.domain.Bet
import fr.bergamof.betgamof.business.domain.BetChange
import fr.bergamof.betgamof.business.domain.DisciplineRule
import fr.bergamof.betgamof.business.domain.Montante
import fr.bergamof.betgamof.business.domain.MontanteConfig
import fr.bergamof.betgamof.business.domain.NewBet
import fr.bergamof.betgamof.business.domain.NewRule
import fr.bergamof.betgamof.business.domain.Settlement
import java.time.Instant

// Driven ports: what the business needs from storage. Implemented by outbound/persistence.

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

    fun update(
        id: Long,
        change: BetChange,
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

    fun create(rule: NewRule): Long

    fun delete(id: Long): Boolean
}
