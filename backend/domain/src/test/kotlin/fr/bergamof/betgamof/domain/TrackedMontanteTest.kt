package fr.bergamof.betgamof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TrackedMontanteTest {
    private val ticket =
        NewBet.of(
            BankrollId(1),
            BetType.SIMPLE,
            "Winamax",
            Money.euros(5),
            1.75,
            T0,
            listOf(Selection.of(null, "Lens – Lyon", "Football", "Ligue 1", "Résultat", "Lens", 1.75)),
        )

    @Test
    fun `a palier stakes the whole capital on the montante's bankroll`() {
        val palier = tracked(montante(), emptyList()).placePalier(ticket, T0)

        assertEquals(Money.euros(160), palier.stake)
        assertEquals(MontanteId(10), palier.montanteId)
        assertEquals(BetStatus.OPEN, palier.status)
    }

    @Test
    fun `a palier on another bankroll is rejected`() {
        val elsewhere =
            NewBet.of(
                BankrollId(2),
                ticket.type,
                ticket.bookmaker,
                ticket.stake,
                ticket.odds,
                ticket.startsAt,
                ticket.selections,
            )

        assertFailsWith<InvalidValueException> { tracked(montante(), emptyList()).placePalier(elsewhere, T0) }
    }

    @Test
    fun `no new palier while one is pending or once the montante is over`() {
        val pending = tracked(montante(), listOf(bet(1, 1.75, Money.euros(160), BetStatus.OPEN, montanteId = 10)))
        assertFailsWith<InvalidTransitionException> { pending.placePalier(ticket, T0) }

        val broken = tracked(montante(), listOf(bet(1, 1.75, Money.euros(160), BetStatus.LOST, montanteId = 10)))
        assertFailsWith<InvalidTransitionException> { broken.placePalier(ticket, T0) }
    }

    @Test
    fun `closing requires an active montante without pending palier`() {
        val active = tracked(montante(), emptyList())
        assertEquals(T0, active.close(T0).closedAt)

        val pending = tracked(montante(), listOf(bet(1, 1.75, Money.euros(160), BetStatus.OPEN, montanteId = 10)))
        assertFailsWith<InvalidTransitionException> { pending.close(T0) }

        val closed = tracked(montante(closedAt = T0), emptyList())
        assertFailsWith<InvalidTransitionException> { closed.close(T0) }
    }

    @Test
    fun `outlook is derived from the state`() {
        val won = bet(1, 1.75, Money.euros(160), BetStatus.WON, montanteId = 10)
        val montante = tracked(montante(montanteConfig(relancesAllowed = 1)), listOf(won))

        assertNotNull(montante.progress)
        assertEquals(true, montante.canRelance)
        assertEquals(Money.euros(160), montante.lossScenario().capitalAfter)
        assertEquals(Money.euros(244), montante.lossScenario().lostAmount)
        assertEquals(montante.state.paliers.size + montante.state.remainingSteps!!, montante.totalPaliers)
        assertNull(montante.endedAt)
        assertNotNull(montante.remainingSuccessProbability)

        val broken = tracked(montante(), listOf(won, bet(2, 1.8, Money.euros(244), BetStatus.LOST, montanteId = 10)))
        assertEquals(
            broken.state.paliers
                .last()
                .bet.settledAt,
            broken.endedAt,
        )
        assertEquals(2, broken.totalPaliers)
        assertEquals(Money.ZERO, broken.lossScenario().capitalAfter)
    }

    @Test
    fun `an excluded stake must be covered by the bankroll`() {
        val poor = BankrollPosition(Money.euros(100), Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO)

        assertFailsWith<InvalidTransitionException> { Montante.start(montanteConfig(excludeStake = true), poor, T0) }
        assertEquals(MontanteId.NEW, Montante.start(montanteConfig(excludeStake = false), poor, T0).id)
    }
}
