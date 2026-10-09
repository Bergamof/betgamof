package fr.bergamof.betgamof.business.service

import fr.bergamof.betgamof.business.ConflictException
import fr.bergamof.betgamof.business.NotFoundException
import fr.bergamof.betgamof.business.domain.BankrollColor
import fr.bergamof.betgamof.business.domain.BankrollSettings
import fr.bergamof.betgamof.business.domain.BetStatus
import fr.bergamof.betgamof.business.domain.DefaultStake
import fr.bergamof.betgamof.business.domain.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BankrollServiceTest {
    private val fixture = ServiceFixture()
    private val bankrolls = fixture.bankrolls

    @Test
    fun `a new bankroll starts at its initial balance`() {
        val id = fixture.bankroll(initialBalance = 500)

        assertEquals(Money.euros(500), bankrolls.get(id).balance)
    }

    @Test
    fun `the stop-loss margin is the distance between balance and stop-loss`() {
        val id = fixture.bankroll(initialBalance = 1000, stopLoss = Money.euros(900))

        assertEquals(Money.euros(100), bankrolls.get(id).stopLossMargin)
    }

    @Test
    fun `settled bets move the balance and the profit`() {
        val id = fixture.bankroll()
        fixture.placeAndSettle(fixture.newBet(id, stake = 25, odds = 1.72), BetStatus.WON)

        val view = bankrolls.get(id)

        assertEquals(Money.euros(1018), view.balance)
        assertEquals(Money.euros(18), view.profit)
        assertEquals(1, view.betCount)
    }

    @Test
    fun `bookmakers are listed from the most used`() {
        val id = fixture.bankroll()
        fixture.bets.create(fixture.newBet(id, bookmaker = "Unibet"))
        fixture.bets.create(fixture.newBet(id, bookmaker = "Winamax"))
        fixture.bets.create(fixture.newBet(id, bookmaker = "Winamax"))

        assertEquals(listOf("Winamax", "Unibet"), bankrolls.get(id).bookmakers)
    }

    @Test
    fun `list returns every bankroll`() {
        fixture.bankroll()
        fixture.bankroll()

        assertEquals(2, bankrolls.list().size)
    }

    @Test
    fun `update replaces the settings`() {
        val id = fixture.bankroll()

        val view = bankrolls.update(id, BankrollSettings("Fun", BankrollColor.CIEL, Money.euros(250), null, 0.1, DefaultStake.Percent(2.0)))

        assertEquals("Fun", view.name)
        assertEquals(DefaultStake.Percent(2.0), view.defaultStake)
    }

    @Test
    fun `updating an unknown bankroll is not found`() {
        assertFailsWith<NotFoundException> {
            bankrolls.update(42, BankrollSettings("Fun", BankrollColor.CIEL, Money.euros(250), null, 0.1, null))
        }
    }

    @Test
    fun `an empty bankroll can be deleted`() {
        val id = fixture.bankroll()

        bankrolls.delete(id)

        assertTrue(bankrolls.list().isEmpty())
    }

    @Test
    fun `a bankroll with bets cannot be deleted`() {
        val id = fixture.bankroll()
        fixture.bets.create(fixture.newBet(id))

        assertFailsWith<ConflictException> { bankrolls.delete(id) }
    }

    @Test
    fun `a bankroll with a montante cannot be deleted`() {
        val id = fixture.bankroll()
        fixture.montantes.create(fixture.montanteConfig(id))

        assertFailsWith<ConflictException> { bankrolls.delete(id) }
    }

    @Test
    fun `unknown bankrolls are not found`() {
        assertFailsWith<NotFoundException> { bankrolls.get(42) }
        assertFailsWith<NotFoundException> { bankrolls.delete(42) }
    }
}
