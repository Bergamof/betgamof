package fr.bergamof.betgamof.business.port.outbound

import fr.bergamof.betgamof.business.domain.BetStatus
import fr.bergamof.betgamof.business.domain.Money
import fr.bergamof.betgamof.business.domain.OddsRange
import fr.bergamof.betgamof.business.domain.Period
import fr.bergamof.betgamof.business.domain.T0
import fr.bergamof.betgamof.business.domain.bet
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class BetCriteriaTest {
    // Won direct football bet on bankroll 1, placed at T0 + 1 h, starting 30 min later, settled 50 min after placement.
    private val won = bet(1, 1.8, Money.euros(10), BetStatus.WON)
    private val placed = won.placedAt

    @TestFactory
    fun `each criterion selects the bet or not`() =
        mapOf(
            "no criterion" to (BetCriteria() to true),
            "same bankroll" to (BetCriteria(bankrollId = 1) to true),
            "other bankroll" to (BetCriteria(bankrollId = 2) to false),
            "montante bets" to (BetCriteria(montanteIds = setOf(10)) to false),
            "direct bets" to (BetCriteria(directOnly = true) to true),
            "same status" to (BetCriteria(status = BetStatus.WON) to true),
            "other status" to (BetCriteria(status = BetStatus.OPEN) to false),
            "decided bets" to (BetCriteria(decidedOnly = true) to true),
            "sport ignoring case" to (BetCriteria(sport = "FOOTBALL") to true),
            "other sport" to (BetCriteria(sport = "Tennis") to false),
            "bookmaker ignoring case" to (BetCriteria(bookmaker = "winamax") to true),
            "text in the event" to (BetCriteria(text = "lyon") to true),
            "text in the bookmaker" to (BetCriteria(text = "WINA") to true),
            "absent text" to (BetCriteria(text = "Nadal") to false),
            "same odds range" to (BetCriteria(oddsRange = OddsRange.MEDIUM) to true),
            "other odds range" to (BetCriteria(oddsRange = OddsRange.SAFE) to false),
            "placed in period" to (BetCriteria(placed = Period(from = placed)) to true),
            "placed before period" to (BetCriteria(placed = Period(from = placed.plusSeconds(1))) to false),
            "starts in period" to (BetCriteria(starts = Period(until = placed.plusSeconds(1_801))) to true),
            "settled in period" to (BetCriteria(settled = Period(from = placed.plusSeconds(3_000))) to true),
            "settled after period" to (BetCriteria(settled = Period(until = placed)) to false),
        ).map { (name, case) -> dynamicTest(name) { assertEquals(case.second, case.first.matches(won)) } }

    @Test
    fun `a pending bet is in no settlement period`() {
        assertEquals(false, BetCriteria(settled = Period.ALL).matches(bet(2, 1.8, Money.euros(10), BetStatus.OPEN)))
    }

    @Test
    fun `orders sort by placement, start or settlement`() {
        val first = bet(1, 1.8, Money.euros(10), BetStatus.WON, placedAt = T0)
        val second = bet(2, 1.8, Money.euros(10), BetStatus.WON, placedAt = T0.plusSeconds(60))
        val open = bet(3, 1.8, Money.euros(10), BetStatus.OPEN, placedAt = T0.plusSeconds(120))
        val bets = listOf(second, open, first)

        assertEquals(listOf(1L, 2, 3), bets.sortedWith(BetOrder.CHRONOLOGICAL.comparator).map { it.id })
        assertEquals(listOf(3L, 2, 1), bets.sortedWith(BetOrder.LATEST_START.comparator).map { it.id })
        assertEquals(listOf(2L, 1, 3), bets.sortedWith(BetOrder.LATEST_SETTLED.comparator).map { it.id })
    }
}
