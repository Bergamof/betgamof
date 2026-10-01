package fr.bergamof.betgamof.business.service

import fr.bergamof.betgamof.business.ConflictException
import fr.bergamof.betgamof.business.NotFoundException
import fr.bergamof.betgamof.business.domain.BetStatus
import fr.bergamof.betgamof.business.domain.Money
import fr.bergamof.betgamof.business.domain.MontanteMode
import fr.bergamof.betgamof.business.domain.MontanteStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MontanteServiceTest {
    private val fixture = ServiceFixture()
    private val montantes = fixture.montantes
    private val bankrollId = fixture.bankroll()

    private fun palier(
        montanteId: Long,
        odds: Double = 1.75,
    ) = fixture.bets.create(fixture.newBet(bankrollId, odds = odds, montanteId = montanteId))

    private fun wonPalier(montanteId: Long) =
        fixture.placeAndSettle(fixture.newBet(bankrollId, odds = 1.75, montanteId = montanteId), BetStatus.WON)

    @Test
    fun `preview plans the paliers and the bankroll after launch`() {
        val preview = montantes.preview(fixture.montanteConfig(bankrollId))

        assertEquals(Money.euros(480), preview.target)
        assertEquals(3, preview.steps.size)
        assertEquals(Money.euros(1000), preview.bankrollBalance)
        assertEquals(Money.euros(840), preview.bankrollBalanceAfterLaunch)
    }

    @Test
    fun `a montante kept in the bankroll does not lower the balance at launch`() {
        val preview = montantes.preview(fixture.montanteConfig(bankrollId, excludeStake = false))

        assertEquals(Money.euros(1000), preview.bankrollBalanceAfterLaunch)
    }

    @Test
    fun `previewing on an unknown bankroll is not found`() {
        assertFailsWith<NotFoundException> { montantes.preview(fixture.montanteConfig(42)) }
    }

    @Test
    fun `a new montante is active at its first palier`() {
        val montante = montantes.create(fixture.montanteConfig(bankrollId))

        assertEquals(MontanteStatus.ACTIVE, montante.status)
        assertEquals(1, montante.currentPalier)
        assertEquals(Money.euros(160), montante.nextStep?.stake)
        assertEquals(0.0, montante.progress)
    }

    @Test
    fun `an excluded stake must be covered by the bankroll`() {
        val poor = fixture.bankroll(initialBalance = 100)

        assertFailsWith<ConflictException> { montantes.create(fixture.montanteConfig(poor)) }
    }

    @Test
    fun `a stake kept in the bankroll needs no coverage`() {
        val poor = fixture.bankroll(initialBalance = 100)

        assertEquals(MontanteStatus.ACTIVE, montantes.create(fixture.montanteConfig(poor, excludeStake = false)).status)
    }

    @Test
    fun `a won palier grows the capital and secures part of the gain`() {
        val montante = montantes.create(fixture.montanteConfig(bankrollId))
        wonPalier(montante.id)

        val view = montantes.get(montante.id)

        assertEquals(Money.euros(244), view.capital)
        assertEquals(Money.euros(36), view.secured)
        assertEquals(1, view.paliers.size)
        assertEquals(2, view.currentPalier)
    }

    @Test
    fun `the next step shows the outcome of the pending palier`() {
        val montante = montantes.create(fixture.montanteConfig(bankrollId, relancesAllowed = 1))
        palier(montante.id, odds = 2.0)

        val next = assertNotNull(montantes.get(montante.id).nextStep)

        assertEquals(Money.euros(272), next.capitalIfWon)
        assertEquals(Money.euros(48), next.securedIfWon)
        assertEquals(true, next.ifLost.relance)
        assertEquals(Money.euros(160), next.ifLost.capitalAfter)
        assertNotNull(next.openBet)
    }

    @Test
    fun `without relance a lost palier loses the capital`() {
        val montante = montantes.create(fixture.montanteConfig(bankrollId))

        val ifLost = assertNotNull(montante.nextStep).ifLost

        assertEquals(false, ifLost.relance)
        assertEquals(Money.ZERO, ifLost.capitalAfter)
        assertEquals(Money.euros(160), ifLost.lostAmount)
    }

    @Test
    fun `a finished montante has no next step and is closed at its last palier`() {
        val montante = montantes.create(fixture.montanteConfig(bankrollId))
        val lost = fixture.placeAndSettle(fixture.newBet(bankrollId, montanteId = montante.id), BetStatus.LOST)

        val view = montantes.get(montante.id)

        assertEquals(MontanteStatus.BROKEN, view.status)
        assertNull(view.nextStep)
        assertEquals(lost.settledAt, view.closedAt)
        assertEquals(1, view.totalPaliers)
    }

    @Test
    fun `free mode has neither target nor planned total`() {
        val montante = montantes.create(fixture.montanteConfig(bankrollId, mode = MontanteMode.FREE))

        assertNull(montante.target)
        assertNull(montante.totalPaliers)
        assertNull(montante.successProbability)
    }

    @Test
    fun `active montantes are listed first, newest first`() {
        val first = montantes.create(fixture.montanteConfig(bankrollId))
        val second = montantes.create(fixture.montanteConfig(bankrollId))
        montantes.close(second.id)
        val third = montantes.create(fixture.montanteConfig(bankrollId))

        assertEquals(listOf(third.id, first.id, second.id), montantes.list().map { it.id })
    }

    @Test
    fun `closing a montante brings its balance back to the bankroll`() {
        val montante = montantes.create(fixture.montanteConfig(bankrollId))
        wonPalier(montante.id)

        assertEquals(MontanteStatus.CLOSED, montantes.close(montante.id).status)
        // 1000 - 160 engaged + 36 secured + 244 capital back.
        assertEquals(Money.euros(1120), fixture.bankrolls.get(bankrollId).balance)
    }

    @Test
    fun `a montante with a pending palier cannot be closed`() {
        val montante = montantes.create(fixture.montanteConfig(bankrollId))
        palier(montante.id)

        assertFailsWith<ConflictException> { montantes.close(montante.id) }
    }

    @Test
    fun `a finished montante cannot be closed again`() {
        val montante = montantes.create(fixture.montanteConfig(bankrollId))
        montantes.close(montante.id)

        assertFailsWith<ConflictException> { montantes.close(montante.id) }
    }

    @Test
    fun `unknown montantes are not found`() {
        assertFailsWith<NotFoundException> { montantes.get(42) }
    }
}
