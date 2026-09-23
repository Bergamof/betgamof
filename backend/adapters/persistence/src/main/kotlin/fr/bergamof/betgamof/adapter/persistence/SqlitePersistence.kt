package fr.bergamof.betgamof.adapter.persistence

import fr.bergamof.betgamof.application.port.outbound.BankrollRepository
import fr.bergamof.betgamof.application.port.outbound.BetRepository
import fr.bergamof.betgamof.application.port.outbound.MontanteRepository
import fr.bergamof.betgamof.application.port.outbound.RuleRepository

/** Opens (and migrates) the SQLite file at [path] and exposes the repository ports backed by it. */
class SqlitePersistence(
    path: String,
) {
    private val db = DatabaseFactory.connect(path)

    val bankrolls: BankrollRepository = ExposedBankrollRepository(db)
    val bets: BetRepository = ExposedBetRepository(db)
    val montantes: MontanteRepository = ExposedMontanteRepository(db)
    val rules: RuleRepository = ExposedRuleRepository(db)
}
