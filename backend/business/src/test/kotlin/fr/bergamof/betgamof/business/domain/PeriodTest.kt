package fr.bergamof.betgamof.business.domain

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeriodTest {
    private val period = Period(T0, T0.plusSeconds(60))

    @Test
    fun `a period includes its start and excludes its end`() {
        assertTrue(T0 in period)
        assertFalse(T0.plusSeconds(60) in period)
        assertFalse(T0.minusSeconds(1) in period)
    }

    @Test
    fun `an open period contains every instant`() {
        assertTrue(T0 in Period.ALL)
    }

    @Test
    fun `the last days end now`() {
        assertEquals(Period(from = T0.minus(Duration.ofDays(30))), Period.last(Duration.ofDays(30), T0))
    }

    @Test
    fun `a period cannot end before it starts`() {
        val error = assertFailsWith<IllegalArgumentException> { Period(T0, T0.minusSeconds(1)) }

        assertEquals("Le début de la période doit précéder sa fin", error.message)
    }

    @Test
    fun `a page has a positive size and offset`() {
        assertEquals("La taille de page doit être positive", assertFailsWith<IllegalArgumentException> { PageRequest(0) }.message)
        assertEquals(
            "Le décalage de page doit être positif ou nul",
            assertFailsWith<IllegalArgumentException> { PageRequest(10, -1) }.message,
        )
    }

    @Test
    fun `bet totals add up settled results and pending stakes`() {
        val bets =
            listOf(
                bet(1, 2.0, Money.euros(10), BetStatus.WON),
                bet(2, 2.0, Money.euros(5), BetStatus.LOST),
                bet(3, 2.0, Money.euros(7), BetStatus.OPEN),
            )

        assertEquals(BetTotals(Money.euros(15), Money.euros(5), Money.euros(7)), BetTotals.of(bets))
    }
}
