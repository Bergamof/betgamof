package fr.bergamof.betgamof.outbound.persistence.sqlite

import fr.bergamof.betgamof.business.domain.BankrollColor
import fr.bergamof.betgamof.business.domain.BankrollSettings
import fr.bergamof.betgamof.business.domain.BetStatus
import fr.bergamof.betgamof.business.domain.BetTotals
import fr.bergamof.betgamof.business.domain.BetType
import fr.bergamof.betgamof.business.domain.Money
import fr.bergamof.betgamof.business.domain.MontanteConfig
import fr.bergamof.betgamof.business.domain.MontanteMode
import fr.bergamof.betgamof.business.domain.NewBet
import fr.bergamof.betgamof.business.domain.OddsRange
import fr.bergamof.betgamof.business.domain.PageRequest
import fr.bergamof.betgamof.business.domain.Period
import fr.bergamof.betgamof.business.domain.Selection
import fr.bergamof.betgamof.business.domain.Settlement
import fr.bergamof.betgamof.business.port.outbound.BetCriteria
import fr.bergamof.betgamof.business.port.outbound.BetOrder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract of the SQL translation: for every criterion, SQLite must return exactly the bets that
 * [BetCriteria.matches] selects, in the order of [BetOrder.comparator].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BetQueriesTest {
    // Not a @TempDir: JUnit may clean it before the dynamic tests of a @TestFactory run.
    private val dir: Path = Files.createTempDirectory("betgamof-bet-queries")

    @AfterAll
    fun deleteDatabase() {
        dir.toFile().deleteRecursively()
    }

    private val t0 = Instant.parse("2026-09-01T18:00:00Z")
    private val persistence by lazy { SqlitePersistence(dir.resolve("bets.db").toString()) }
    private val bets get() = persistence.bets

    private val main by lazy { bankroll("Principale") }
    private val funBankroll by lazy { bankroll("Fun") }
    private val montanteId by lazy {
        persistence.montantes.create(MontanteConfig("M", main, Money.euros(50), 1.8, MontanteMode.FREE, null, null, false, 20, 0), t0)
    }

    private fun bankroll(name: String) =
        persistence.bankrolls.create(BankrollSettings(name, BankrollColor.GAZON, Money.euros(100), null, 0.25, null), t0)

    private fun selection(
        event: String,
        sport: String,
        odds: Double,
    ) = Selection(null, event, sport, "Ligue 1", "Résultat", "Domicile", odds)

    /** Places a bet [hours] after t0 (with a fractional second), settled one hour later unless [settlement] is null. */
    private fun place(
        bankrollId: Long,
        hours: Long,
        odds: Double,
        settlement: Settlement?,
        bookmaker: String = "Winamax",
        sport: String = "Football",
        event: String = "Lens – Lyon",
        montante: Long? = null,
        stake: Double = 10.0,
    ) {
        val placedAt = t0.plusSeconds(hours * 3_600).plusMillis(hours * 7)
        val id =
            bets.create(
                NewBet(
                    bankrollId,
                    montante,
                    BetType.SIMPLE,
                    bookmaker,
                    Money.euros(stake),
                    odds,
                    placedAt.plusSeconds(1_800),
                    listOf(selection(event, sport, odds)),
                ),
                placedAt,
            )
        settlement?.let { bets.settle(id, it, placedAt.plusSeconds(3_600)) }
    }

    private val dataset by lazy {
        place(main, 0, 1.8, Settlement(BetStatus.WON, null))
        place(main, 1, 2.4, Settlement(BetStatus.LOST, null), bookmaker = "Unibet", sport = "Tennis", event = "Nadal – Alcaraz")
        place(main, 2, 1.33, Settlement(BetStatus.CASHOUT, Money.euros(11.37)), stake = 12.35)
        place(main, 3, 3.5, null, bookmaker = "Betclic")
        place(funBankroll, 4, 1.72, Settlement(BetStatus.VOID, null), sport = "Rugby", event = "Toulouse – 50%_off")
        place(main, 5, 1.75, Settlement(BetStatus.WON, null), montante = montanteId, stake = 50.0)
        place(main, 6, 1.9, null, montante = montanteId, stake = 71.25)
        bets.find(BetCriteria())
    }

    private fun expected(criteria: BetCriteria) =
        dataset
            .filter(criteria::matches)
            .sortedWith(criteria.order.comparator)
            .let { sorted -> criteria.page?.let { sorted.drop(it.offset).take(it.limit) } ?: sorted }
            .map { it.id }

    @TestFactory
    fun `SQLite selects the same bets as the business criteria`() =
        mapOf(
            "everything" to BetCriteria(),
            "one bankroll" to BetCriteria(bankrollId = funBankroll),
            "montante bets" to BetCriteria(montanteIds = setOf(montanteId)),
            "no montante" to BetCriteria(montanteIds = emptySet()),
            "direct bets" to BetCriteria(directOnly = true),
            "status" to BetCriteria(status = BetStatus.CASHOUT),
            "decided" to BetCriteria(decidedOnly = true),
            "sport ignoring case" to BetCriteria(sport = "tennis"),
            "bookmaker ignoring case" to BetCriteria(bookmaker = "BETCLIC"),
            "text ignoring case" to BetCriteria(text = " ALCARAZ "),
            "text with LIKE wildcards" to BetCriteria(text = "50%_"),
            "wildcards taken literally" to BetCriteria(text = "L%n"),
            "odds range" to BetCriteria(oddsRange = OddsRange.MEDIUM),
            "placed period within a second" to BetCriteria(placed = Period(t0.plusSeconds(3_600).plusMillis(7), t0.plusSeconds(10_800))),
            "starts period" to BetCriteria(starts = Period(from = t0.plusSeconds(4 * 3_600))),
            "settled period" to BetCriteria(settled = Period(until = t0.plusSeconds(3 * 3_600))),
            "combined" to BetCriteria(bankrollId = main, directOnly = true, decidedOnly = true),
            "latest start first" to BetCriteria(order = BetOrder.LATEST_START),
            "latest settled first" to BetCriteria(order = BetOrder.LATEST_SETTLED),
            "page" to BetCriteria(order = BetOrder.LATEST_START, page = PageRequest(limit = 2, offset = 1)),
        ).map { (name, criteria) ->
            dynamicTest(name) {
                assertEquals(expected(criteria), bets.find(criteria).map { it.id })
                assertEquals(dataset.count(criteria::matches), bets.count(criteria))
            }
        }

    @Test
    fun `found bets keep their selections`() {
        assertEquals(dataset, bets.find(BetCriteria(page = PageRequest(limit = 100))))
    }

    @Test
    fun `direct totals match the business totals of each bankroll`() {
        val direct = dataset.filter { it.montanteId == null }

        assertEquals(direct.groupBy { it.bankrollId }.mapValues { BetTotals.of(it.value) }, bets.directTotalsByBankroll())
    }

    @Test
    fun `bookmakers are ranked by usage`() {
        dataset

        assertEquals(listOf("Winamax", "Betclic", "Unibet"), bets.bookmakersByUsage(main))
    }
}
