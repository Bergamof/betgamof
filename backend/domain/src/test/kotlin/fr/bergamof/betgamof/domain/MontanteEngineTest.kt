package fr.bergamof.betgamof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MontanteEngineTest {
    @Test
    fun `winning a palier secures part of the gain and re-stakes the rest`() {
        val state = MontanteEngine.replay(montante(), listOf(bet(1, 1.75, Money.euros(160), BetStatus.WON, montanteId = 10)))

        // Gain 120 €, 30 % secured (36 €), 244 € re-staked — the example shown on the creation screen.
        assertEquals(Money.euros(36), state.secured)
        assertEquals(Money.euros(244), state.capital)
        assertEquals(MontanteStatus.ACTIVE, state.status)
        assertEquals(2, state.currentPalierNumber)
    }

    @Test
    fun `reaching the objective ends the montante as succeeded`() {
        val config = montanteConfig(targetMultiplier = 1.5, securePct = 0)
        val state = MontanteEngine.replay(montante(config), listOf(bet(1, 1.6, Money.euros(160), BetStatus.WON, montanteId = 10)))

        assertEquals(MontanteStatus.SUCCEEDED, state.status)
        assertEquals(Money.euros(256), state.capital)
    }

    @Test
    fun `a loss without relance breaks the montante but keeps secured gains`() {
        val bets =
            listOf(
                bet(1, 1.75, Money.euros(160), BetStatus.WON, montanteId = 10),
                bet(2, 1.8, Money.euros(244), BetStatus.LOST, montanteId = 10),
            )
        val state = MontanteEngine.replay(montante(), bets)

        assertEquals(MontanteStatus.BROKEN, state.status)
        assertEquals(Money.ZERO, state.capital)
        assertEquals(Money.euros(36), state.secured)
        assertEquals(Money.euros(-124), state.result)
    }

    @Test
    fun `a relance restarts from the starting capital and engages it again`() {
        val bets =
            listOf(
                bet(1, 1.75, Money.euros(160), BetStatus.WON, montanteId = 10),
                bet(2, 1.8, Money.euros(244), BetStatus.LOST, montanteId = 10),
            )
        val state = MontanteEngine.replay(montante(montanteConfig(relancesAllowed = 1)), bets)

        assertEquals(MontanteStatus.ACTIVE, state.status)
        assertEquals(Money.euros(160), state.capital)
        assertEquals(Money.euros(320), state.engaged)
        assertEquals(1, state.relancesUsed)
        assertEquals(0, state.winsInRun)
        assertTrue(state.nextIsRelance)
    }

    @Test
    fun `steps mode succeeds after the planned number of won paliers`() {
        val config = montanteConfig(mode = MontanteMode.STEPS, targetMultiplier = null, stepCount = 2)
        val bets =
            listOf(
                bet(1, 1.75, Money.euros(160), BetStatus.WON, montanteId = 10),
                bet(2, 1.75, Money.euros(244), BetStatus.WON, montanteId = 10),
            )

        assertEquals(MontanteStatus.SUCCEEDED, MontanteEngine.replay(montante(config), bets).status)
    }

    @Test
    fun `an open bet is exposed as the current palier`() {
        val open = bet(1, 2.1, Money.euros(160), BetStatus.OPEN, montanteId = 10)
        val state = MontanteEngine.replay(montante(), listOf(open))

        assertEquals(open, state.openBet)
        assertEquals(1, state.currentPalierNumber)
    }

    @Test
    fun `a manually closed montante returns its capital`() {
        val state = MontanteEngine.replay(montante(closedAt = T0.plusSeconds(99_999)), emptyList())

        assertEquals(MontanteStatus.CLOSED, state.status)
        assertEquals(Money.ZERO, state.result)
    }

    @Test
    fun `free mode has no target`() {
        val state = MontanteEngine.replay(montante(montanteConfig(mode = MontanteMode.FREE, targetMultiplier = null)), emptyList())

        assertNull(state.target)
        assertNull(state.remainingSteps)
    }
}
