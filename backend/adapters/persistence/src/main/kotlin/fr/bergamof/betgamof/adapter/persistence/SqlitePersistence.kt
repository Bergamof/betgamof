package fr.bergamof.betgamof.adapter.persistence

import fr.bergamof.betgamof.application.port.outbound.BankrollRepository
import fr.bergamof.betgamof.application.port.outbound.BetRepository
import fr.bergamof.betgamof.application.port.outbound.MontanteRepository
import fr.bergamof.betgamof.application.port.outbound.RuleRepository
import fr.bergamof.betgamof.application.port.outbound.TransactionRunner

/** Opens (and migrates) the SQLite file at [path] and exposes the persistence ports backed by it. */
class SqlitePersistence(
    path: String,
) {
    private val runner = SqliteTransactionRunner(DatabaseFactory.connect(path), SqliteTransactionRunner.lockFor(path))

    val bankrolls: BankrollRepository = ExposedBankrollRepository(runner)
    val bets: BetRepository = ExposedBetRepository(runner)
    val montantes: MontanteRepository = ExposedMontanteRepository(runner)
    val rules: RuleRepository = ExposedRuleRepository(runner)
    val transactions: TransactionRunner = runner
}
