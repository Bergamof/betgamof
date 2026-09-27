package fr.bergamof.betgamof.outbound.persistence.sqlite

import fr.bergamof.betgamof.business.domain.BankrollColor
import fr.bergamof.betgamof.business.domain.BankrollSettings
import fr.bergamof.betgamof.business.domain.BetChange
import fr.bergamof.betgamof.business.domain.BetStatus
import fr.bergamof.betgamof.business.domain.BetType
import fr.bergamof.betgamof.business.domain.DisciplineRule
import fr.bergamof.betgamof.business.domain.Money
import fr.bergamof.betgamof.business.domain.MontanteConfig
import fr.bergamof.betgamof.business.domain.MontanteMode
import fr.bergamof.betgamof.business.domain.NewBet
import fr.bergamof.betgamof.business.domain.NewRule
import fr.bergamof.betgamof.business.domain.RuleKind
import fr.bergamof.betgamof.business.domain.Selection
import fr.bergamof.betgamof.business.domain.Settlement
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlitePersistenceTest {
    @TempDir
    lateinit var dir: Path

    private val now = Instant.parse("2026-09-11T16:00:00Z")
    private val settings = BankrollSettings("Principale", BankrollColor.CIEL, Money.euros(1088.5), Money.euros(900), 0.25, Money.euros(5))
    private val selections =
        listOf(
            Selection("a", "A – B", "Football", "Ligue 1", "Résultat final", "A", 1.5),
            Selection(null, "C – D", "Tennis", "ATP", "Vainqueur", "C", 2.0),
        )

    private val persistence by lazy { SqlitePersistence(dir.resolve("data/test.db").toString()) }
    private val bankrollId by lazy { persistence.bankrolls.create(settings, now) }

    private fun montanteId(mode: MontanteMode = MontanteMode.STEPS) =
        persistence.montantes.create(MontanteConfig("M", bankrollId, Money.euros(50), 1.8, mode, null, 4, true, 20, 1), now)

    private fun betId(montanteId: Long? = null) =
        persistence.bets.create(
            NewBet(bankrollId, montanteId, BetType.COMBINE, " Unibet ", Money.euros(50), 3.0, now.plusSeconds(60), selections),
            now,
        )

    @Test
    fun `a bankroll round-trips with its settings and creation date`() {
        val bankroll = assertNotNull(persistence.bankrolls.find(bankrollId))

        assertEquals(settings, bankroll.settings)
        assertEquals(now, bankroll.createdAt)
    }

    @Test
    fun `a bankroll can be updated then deleted`() {
        val renamed = settings.copy(name = "Fun", stopLoss = null)

        assertTrue(persistence.bankrolls.update(bankrollId, renamed))
        assertEquals(listOf(renamed), persistence.bankrolls.findAll().map { it.settings })
        assertTrue(persistence.bankrolls.delete(bankrollId))
        assertNull(persistence.bankrolls.find(bankrollId))
    }

    @Test
    fun `unknown bankrolls are neither updated nor deleted`() {
        assertFalse(persistence.bankrolls.update(42, settings))
        assertFalse(persistence.bankrolls.delete(42))
    }

    @Test
    fun `a bet keeps its selections in order, its montante link and a trimmed bookmaker`() {
        val montanteId = montanteId()
        val bet = assertNotNull(persistence.bets.find(betId(montanteId)))

        assertEquals(selections, bet.selections)
        assertEquals(montanteId, bet.montanteId)
        assertEquals("Unibet", bet.bookmaker)
        assertEquals(BetStatus.OPEN, bet.status)
        assertEquals(now, bet.placedAt)
    }

    @Test
    fun `a settlement records status, cash-out and date`() {
        val id = betId()

        persistence.bets.settle(id, Settlement(BetStatus.CASHOUT, Money.euros(72.5)), now.plusSeconds(120))

        val bet = assertNotNull(persistence.bets.find(id))
        assertEquals(BetStatus.CASHOUT, bet.status)
        assertEquals(Money.euros(72.5), bet.cashout)
        assertEquals(now.plusSeconds(120), bet.settledAt)
    }

    @Test
    fun `a bet change replaces stake, odds and bookmaker`() {
        val id = betId()

        assertTrue(persistence.bets.update(id, BetChange(Money.euros(20), 2.2, "Winamax ")))

        val bet = assertNotNull(persistence.bets.find(id))
        assertEquals(Money.euros(20), bet.stake)
        assertEquals(2.2, bet.odds)
        assertEquals("Winamax", bet.bookmaker)
    }

    @Test
    fun `deleting a bet deletes its selections`() {
        val id = betId()

        assertTrue(persistence.bets.delete(id))
        assertEquals(emptyList(), persistence.bets.findAll())
        assertFalse(persistence.bets.delete(id))
    }

    @Test
    fun `a montante round-trips and can be closed`() {
        val id = montanteId(MontanteMode.FREE)
        assertNull(persistence.montantes.find(id)?.closedAt)

        assertTrue(persistence.montantes.close(id, now))

        val montante = assertNotNull(persistence.montantes.findAll().single())
        assertEquals(MontanteMode.FREE, montante.config.mode)
        assertEquals(now, montante.closedAt)
    }

    @Test
    fun `rules are created, listed and deleted`() {
        val id = persistence.rules.create(NewRule(RuleKind.MAX_STAKE_PCT, 3))

        assertEquals(listOf(DisciplineRule(id, RuleKind.MAX_STAKE_PCT, 3)), persistence.rules.findAll())
        assertTrue(persistence.rules.delete(id))
        assertEquals(emptyList(), persistence.rules.findAll())
    }

    @Test
    fun `data survives reopening the database`() {
        bankrollId
        val reopened = SqlitePersistence(dir.resolve("data/test.db").toString())

        assertEquals(1, reopened.bankrolls.findAll().size)
    }
}
