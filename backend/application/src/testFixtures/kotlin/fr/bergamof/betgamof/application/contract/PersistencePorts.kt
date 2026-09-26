package fr.bergamof.betgamof.application.contract

import fr.bergamof.betgamof.application.port.outbound.BankrollRepository
import fr.bergamof.betgamof.application.port.outbound.BetRepository
import fr.bergamof.betgamof.application.port.outbound.MontanteRepository
import fr.bergamof.betgamof.application.port.outbound.RuleRepository
import fr.bergamof.betgamof.application.port.outbound.TransactionRunner

/**
 * Every driven persistence port of one store. The contract suites need them together: a bet can only
 * be stored once its bankroll (and montante) exist, and transactions span all repositories.
 */
interface PersistencePorts {
    val bankrolls: BankrollRepository
    val bets: BetRepository
    val montantes: MontanteRepository
    val rules: RuleRepository
    val transactions: TransactionRunner
}
