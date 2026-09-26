package fr.bergamof.betgamof.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class BankrollLedgerTest {
    private val bankroll =
        Bankroll(BankrollId(1), BankrollSettings.of("Principale", BankrollColor.GAZON, Money.euros(1000), Money.euros(900)), T0)

    @Test
    fun `balance adds settled direct bets and ignores open ones`() {
        val bets =
            listOf(
                bet(1, 2.0, Money.euros(50), BetStatus.WON),
                bet(2, 1.8, Money.euros(20), BetStatus.LOST),
                bet(3, 1.5, Money.euros(30), BetStatus.OPEN),
            )
        val position = BankrollLedger.position(bankroll, bets, emptyList())

        assertEquals(Money.euros(1030), position.balance)
        assertEquals(Money.euros(30), position.openStake)
        assertEquals(30.0 / 70.0, position.roi, 1e-9)
    }

    @Test
    fun `an excluded montante stake leaves the bankroll until the montante ends`() {
        val active = montante()
        val won = listOf(bet(1, 1.75, Money.euros(160), BetStatus.WON, montanteId = 10))
        val position = BankrollLedger.position(bankroll, won, listOf(tracked(active, won)))

        // 160 € out, 36 € secured back: 1000 - 160 + 36.
        assertEquals(Money.euros(876), position.balance)
        assertEquals(Money.euros(160), position.outsideMontantes)
    }

    @Test
    fun `closing an excluded montante brings its final balance back`() {
        val closed = montante(closedAt = T0.plusSeconds(99_999))
        val won = listOf(bet(1, 1.75, Money.euros(160), BetStatus.WON, montanteId = 10))
        val position = BankrollLedger.position(bankroll, won, listOf(tracked(closed, won)))

        assertEquals(Money.euros(1120), position.balance)
        assertEquals(Money.ZERO, position.outsideMontantes)
    }

    @Test
    fun `a montante kept in the bankroll moves the balance live`() {
        val inside = montante(montanteConfig(excludeStake = false))
        val won = listOf(bet(1, 1.75, Money.euros(160), BetStatus.WON, montanteId = 10))
        val position = BankrollLedger.position(bankroll, won, listOf(tracked(inside, won)))

        assertEquals(Money.euros(1120), position.balance)
    }
}
