package fr.bergamof.betgamof.business.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BetTest {
    private val stake = Money.euros(10)

    @Test
    fun `a won bet earns its net odds`() {
        assertEquals(Money.euros(8), bet(1, 1.8, stake, BetStatus.WON).profit)
    }

    @Test
    fun `a lost bet loses its stake`() {
        assertEquals(Money.euros(-10), bet(1, 1.8, stake, BetStatus.LOST).profit)
    }

    @Test
    fun `a cash-out earns the amount returned minus the stake`() {
        assertEquals(Money.euros(2.5), bet(1, 1.8, stake, BetStatus.CASHOUT, cashout = Money.euros(12.5)).profit)
    }

    @Test
    fun `open and void bets earn nothing`() {
        assertEquals(Money.ZERO, bet(1, 1.8, stake, BetStatus.OPEN).profit)
        assertEquals(Money.ZERO, bet(1, 1.8, stake, BetStatus.VOID).profit)
    }

    @Test
    fun `only won and lost bets are decided`() {
        assertTrue(bet(1, 1.8, stake, BetStatus.WON).isDecided)
        assertEquals(false, bet(1, 1.8, stake, BetStatus.VOID).isDecided)
    }

    @Test
    fun `the potential return is stake times odds`() {
        assertEquals(Money.euros(18), bet(1, 1.8, stake, BetStatus.OPEN).potentialReturn)
    }

    @Test
    fun `money is computed in cents without drift`() {
        val total = listOf(Money.euros(0.1), Money.euros(0.2)).sum()

        assertEquals(Money(30), total)
        assertEquals(0.3, total.toEuros())
        assertEquals(Money.euros(-0.3), -total)
        assertEquals(0.5, Money.euros(1) / Money.euros(2))
    }

    @Test
    fun `odds ranges split the odds scale`() {
        assertEquals(OddsRange.SAFE, OddsRange.of(1.2))
        assertEquals(OddsRange.MEDIUM, OddsRange.of(1.5))
        assertEquals(OddsRange.VALUE, OddsRange.of(2.5))
        assertEquals(OddsRange.LONG_SHOT, OddsRange.of(12.0))
    }
}
