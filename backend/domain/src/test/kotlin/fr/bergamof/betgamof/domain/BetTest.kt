package fr.bergamof.betgamof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BetTest {
    private fun newBet(
        stake: Int = 20,
        odds: Double = 1.8,
        bookmaker: String = "  Winamax ",
    ) = NewBet.of(
        bankrollId = BankrollId(1),
        type = BetType.SIMPLE,
        bookmaker = bookmaker,
        stake = Money.euros(stake),
        odds = odds,
        startsAt = T0,
        selections = listOf(Selection.of(" ", " Lens – Lyon ", "Football ", " Ligue 1", "Total de buts", " Plus de 1,5 ", odds)),
    )

    @Test
    fun `a placed bet is pending with normalised text`() {
        val bet = Bet.place(newBet(), T0)

        assertEquals(BetStatus.OPEN, bet.status)
        assertEquals(BetId.NEW, bet.id)
        assertEquals("Winamax", bet.bookmaker)
        assertEquals("Lens – Lyon", bet.selections.single().eventName)
        assertEquals("Plus de 1,5", bet.selections.single().pick)
        assertNull(bet.selections.single().eventId)
    }

    @Test
    fun `invalid terms are rejected as invalid values`() {
        assertFailsWith<InvalidValueException> { newBet(stake = 0) }
        assertFailsWith<InvalidValueException> { newBet(odds = 1.0) }
        assertFailsWith<InvalidValueException> { newBet(bookmaker = "  ") }
        assertFailsWith<InvalidValueException> { Settlement(BetStatus.CASHOUT, null) }
    }

    @Test
    fun `a bet is settled once`() {
        val settled = Bet.place(newBet(), T0).settle(Settlement(BetStatus.WON, null), T0.plusSeconds(60))

        assertEquals(BetStatus.WON, settled.status)
        assertEquals(T0.plusSeconds(60), settled.settledAt)
        assertFailsWith<InvalidTransitionException> { settled.settle(Settlement(BetStatus.LOST, null), T0) }
    }

    @Test
    fun `only a pending bet can be amended and a palier keeps its stake`() {
        val open = Bet.place(newBet(), T0)
        assertEquals(Money.euros(30), open.amend(Money.euros(30), 2.0, " Betclic ").stake)
        assertEquals("Betclic", open.amend(Money.euros(30), 2.0, " Betclic ").bookmaker)
        assertFailsWith<InvalidValueException> { open.amend(Money.euros(30), 0.9, "Betclic") }

        val palier = Bet.place(newBet(), T0, MontanteId(3))
        assertFailsWith<InvalidTransitionException> { palier.amend(Money.euros(99), 2.0, "Winamax") }
        assertEquals(2.0, palier.amend(palier.stake, 2.0, "Winamax").odds)

        val settled = open.settle(Settlement(BetStatus.LOST, null), T0)
        assertFailsWith<InvalidTransitionException> { settled.amend(Money.euros(30), 2.0, "Winamax") }
    }

    @Test
    fun `a settled palier cannot be deleted`() {
        Bet.place(newBet(), T0, MontanteId(3)).ensureDeletable()
        bet(1, 2.0, Money.euros(10), BetStatus.WON).ensureDeletable()

        assertFailsWith<InvalidTransitionException> {
            bet(2, 2.0, Money.euros(10), BetStatus.WON, montanteId = 3).ensureDeletable()
        }
    }

    @Test
    fun `combined bets have no single market`() {
        val combined =
            NewBet.of(
                BankrollId(1),
                BetType.COMBINE,
                "Winamax",
                Money.euros(10),
                3.0,
                T0,
                listOf(
                    Selection.of(null, "A – B", "Football", "L1", "Résultat", "A", 1.5),
                    Selection.of(null, "C – D", "Football", "L1", "Résultat", "C", 2.0),
                ),
            )

        assertNull(Bet.place(combined, T0).market)
    }
}
