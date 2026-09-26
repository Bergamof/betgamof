package fr.bergamof.betgamof.outbound.persistence

import fr.bergamof.betgamof.business.port.outbound.BankrollRepository
import fr.bergamof.betgamof.business.port.outbound.BetRepository
import fr.bergamof.betgamof.business.port.outbound.MontanteRepository
import fr.bergamof.betgamof.business.port.outbound.RuleRepository

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
