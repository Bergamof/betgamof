package fr.bergamof.betgamof.adapter.persistence

import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BankrollSettings
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteConfig
import fr.bergamof.betgamof.domain.MontanteMode
import fr.bergamof.betgamof.domain.NewBet
import fr.bergamof.betgamof.domain.RuleKind
import fr.bergamof.betgamof.domain.Selection
import fr.bergamof.betgamof.domain.Settlement
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlitePersistenceTest {
    @TempDir
    lateinit var dir: Path

    private val now = Instant.parse("2026-09-11T16:00:00Z")

    private fun persistence() = SqlitePersistence(dir.resolve("test.db").toString())

    @Test
    fun `bankrolls round-trip through SQLite`() {
        val persistence = persistence()
        val settings = BankrollSettings("Principale", BankrollColor.CIEL, Money.euros(1088.5), Money.euros(900), 0.25, Money.euros(5))

        val id = persistence.bankrolls.create(settings, now)

        assertEquals(settings, persistence.bankrolls.find(id)?.settings)
        assertEquals(now, persistence.bankrolls.find(id)?.createdAt)
    }

    @Test
    fun `bets keep their selections, settlement and montante link`() {
        val persistence = persistence()
        val bankrollId = persistence.bankrolls.create(BankrollSettings("B", BankrollColor.GAZON, Money.euros(100), null, 0.25, null), now)
        val montanteId =
            persistence.montantes.create(
                MontanteConfig("M", bankrollId, Money.euros(50), 1.8, MontanteMode.STEPS, null, 4, true, 20, 1),
                now,
            )
        val selections =
            listOf(
                Selection("a", "A – B", "Football", "Ligue 1", "Résultat final", "A", 1.5),
                Selection(null, "C – D", "Tennis", "ATP", "Vainqueur", "C", 2.0),
            )
        val betId =
            persistence.bets.create(
                NewBet(bankrollId, montanteId, BetType.COMBINE, "Unibet", Money.euros(50), 3.0, now.plusSeconds(60), selections),
                now,
            )

        persistence.bets.settle(betId, Settlement(BetStatus.CASHOUT, Money.euros(72.5)), now.plusSeconds(120))

        val bet = persistence.bets.find(betId)!!
        assertEquals(selections, bet.selections)
        assertEquals(montanteId, bet.montanteId)
        assertEquals(BetStatus.CASHOUT, bet.status)
        assertEquals(Money.euros(72.5), bet.cashout)
        assertEquals(now.plusSeconds(120), bet.settledAt)
    }

    @Test
    fun `montantes can be closed and rules deleted`() {
        val persistence = persistence()
        val bankrollId = persistence.bankrolls.create(BankrollSettings("B", BankrollColor.GAZON, Money.euros(100), null, 0.25, null), now)
        val montanteId =
            persistence.montantes.create(
                MontanteConfig("M", bankrollId, Money.euros(50), 1.8, MontanteMode.FREE, null, null, false, 0, 0),
                now,
            )
        assertNull(persistence.montantes.find(montanteId)?.closedAt)

        persistence.montantes.close(montanteId, now)
        val ruleId = persistence.rules.create(RuleKind.MAX_STAKE_PCT, 3)

        assertEquals(now, persistence.montantes.find(montanteId)?.closedAt)
        assertEquals(true, persistence.rules.delete(ruleId))
        assertEquals(emptyList(), persistence.rules.findAll())
    }
}
