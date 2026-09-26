package fr.bergamof.betgamof.adapter.persistence

import fr.bergamof.betgamof.application.contract.BankrollRepositoryContract
import fr.bergamof.betgamof.application.contract.BetRepositoryContract
import fr.bergamof.betgamof.application.contract.MontanteRepositoryContract
import fr.bergamof.betgamof.application.contract.PersistencePorts
import fr.bergamof.betgamof.application.contract.RuleRepositoryContract
import fr.bergamof.betgamof.application.contract.TransactionRunnerContract
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

// The SQLite adapter runs the contract suites of the persistence ports, each test on a new database file.

/** Exposes a [SqlitePersistence] as the ports the contract suites work on. */
internal class SqlitePorts(
    persistence: SqlitePersistence,
) : PersistencePorts {
    override val bankrolls = persistence.bankrolls
    override val bets = persistence.bets
    override val montantes = persistence.montantes
    override val rules = persistence.rules
    override val transactions = persistence.transactions
}

private fun openIn(dir: Path) = SqlitePorts(SqlitePersistence(dir.resolve("test.db").toString()))

class SqliteBankrollRepositoryTest : BankrollRepositoryContract() {
    @TempDir
    lateinit var dir: Path

    override fun newPersistence(): PersistencePorts = openIn(dir)
}

class SqliteBetRepositoryTest : BetRepositoryContract() {
    @TempDir
    lateinit var dir: Path

    override fun newPersistence(): PersistencePorts = openIn(dir)
}

class SqliteMontanteRepositoryTest : MontanteRepositoryContract() {
    @TempDir
    lateinit var dir: Path

    override fun newPersistence(): PersistencePorts = openIn(dir)
}

class SqliteRuleRepositoryTest : RuleRepositoryContract() {
    @TempDir
    lateinit var dir: Path

    override fun newPersistence(): PersistencePorts = openIn(dir)
}

class SqliteTransactionRunnerTest : TransactionRunnerContract() {
    @TempDir
    lateinit var dir: Path

    override fun newPersistence(): PersistencePorts = openIn(dir)
}
