package fr.bergamof.betgamof.application.port.outbound

import fr.bergamof.betgamof.domain.Bankroll
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.Bet
import fr.bergamof.betgamof.domain.BetId
import fr.bergamof.betgamof.domain.DisciplineRule
import fr.bergamof.betgamof.domain.Montante
import fr.bergamof.betgamof.domain.MontanteId
import fr.bergamof.betgamof.domain.RuleId

// Driven ports: aggregate storage. Implemented by adapters/persistence.
// Repositories store and load whole aggregates; state changes are decided by the domain.
// `add` ignores the aggregate's id (NEW) and returns it with the id the store assigned; ids are never reused.
// `save` and `remove` expect an existing aggregate: callers look it up with `findById` first.

interface BankrollRepository {
    fun findAll(): List<Bankroll>

    fun findById(id: BankrollId): Bankroll?

    fun add(bankroll: Bankroll): Bankroll

    fun save(bankroll: Bankroll)

    fun remove(id: BankrollId)
}

interface BetRepository {
    fun findAll(): List<Bet>

    fun findById(id: BetId): Bet?

    fun add(bet: Bet): Bet

    fun save(bet: Bet)

    fun remove(id: BetId)
}

interface MontanteRepository {
    fun findAll(): List<Montante>

    fun findById(id: MontanteId): Montante?

    fun add(montante: Montante): Montante

    fun save(montante: Montante)
}

interface RuleRepository {
    fun findAll(): List<DisciplineRule>

    fun findById(id: RuleId): DisciplineRule?

    fun add(rule: DisciplineRule): DisciplineRule

    fun remove(id: RuleId)
}
