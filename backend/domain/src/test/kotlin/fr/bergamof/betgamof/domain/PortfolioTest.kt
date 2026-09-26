package fr.bergamof.betgamof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PortfolioTest {
    private val main = Bankroll(BankrollId(1), BankrollSettings.of(" Principale ", BankrollColor.GAZON, Money.euros(1000)), T0)
    private val spare = Bankroll(BankrollId(2), BankrollSettings.of("Réserve", BankrollColor.CIEL, Money.euros(200)), T0)

    @Test
    fun `settings are normalised and default to quarter Kelly`() {
        assertEquals("Principale", main.settings.name)
        assertEquals(BankrollSettings.DEFAULT_KELLY_FRACTION, main.settings.kellyFraction)
        assertFailsWith<InvalidValueException> { BankrollSettings.of(" ", BankrollColor.GAZON, Money.euros(10)) }
    }

    @Test
    fun `a bankroll with history cannot be deleted`() {
        val portfolio = Portfolio(listOf(main, spare), listOf(bet(1, 2.0, Money.euros(10), BetStatus.WON)), emptyList())

        assertFailsWith<InvalidTransitionException> { portfolio.ensureBankrollDeletable(BankrollId(1)) }
        portfolio.ensureBankrollDeletable(BankrollId(2))
        assertEquals(Money.euros(1210), portfolio.totalBalance())
        assertEquals(Money.euros(200), portfolio.totalBalance(BankrollId(2)))
    }
}
