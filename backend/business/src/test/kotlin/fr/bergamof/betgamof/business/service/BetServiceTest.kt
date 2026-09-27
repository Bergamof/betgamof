package fr.bergamof.betgamof.business.service

import fr.bergamof.betgamof.business.ConflictException
import fr.bergamof.betgamof.business.NotFoundException
import fr.bergamof.betgamof.business.domain.BetChange
import fr.bergamof.betgamof.business.domain.BetStatus
import fr.bergamof.betgamof.business.domain.BetType
import fr.bergamof.betgamof.business.domain.Money
import fr.bergamof.betgamof.business.domain.PageRequest
import fr.bergamof.betgamof.business.domain.Selection
import fr.bergamof.betgamof.business.domain.Settlement
import fr.bergamof.betgamof.business.port.inbound.BetFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BetServiceTest {
    private val fixture = ServiceFixture()
    private val bets = fixture.bets
    private val bankrollId = fixture.bankroll()

    private fun ids(filter: BetFilter) = bets.list(filter).map { it.id }

    @Test
    fun `a new bet is open and labelled from its selection`() {
        val bet = bets.create(fixture.newBet(bankrollId))

        assertEquals(BetStatus.OPEN, bet.status)
        assertEquals("Lens – Lyon · Plus de 1,5", bet.label)
        assertEquals("Principale", bet.bankrollName)
        assertNull(bet.palierNumber)
    }

    @Test
    fun `a combined bet is labelled with its number of selections`() {
        val selections = listOf("A", "B").map { Selection(null, "$it – X", "Football", "L$it", "Résultat", it, 1.5) }
        val request = fixture.newBet(bankrollId).copy(type = BetType.COMBINE, odds = 2.25, selections = selections)

        val bet = bets.create(request)

        assertEquals("Combiné 2 sélections", bet.label)
        assertEquals("LA · LB", bet.competition)
        assertEquals("Combiné", bet.market)
    }

    @Test
    fun `a system bet is labelled as such`() {
        val selections = listOf("A", "B", "C").map { Selection(null, "$it – X", "Football", "L1", "Résultat", it, 1.5) }

        val bet = bets.create(fixture.newBet(bankrollId).copy(type = BetType.SYSTEME, selections = selections))

        assertEquals("Système 3 sélections", bet.label)
    }

    @Test
    fun `a bet on an unknown bankroll is not found`() {
        assertFailsWith<NotFoundException> { bets.create(fixture.newBet(42)) }
    }

    @Test
    fun `settling a bet records its outcome`() {
        val bet = fixture.placeAndSettle(fixture.newBet(bankrollId, stake = 10, odds = 2.0), BetStatus.WON)

        assertEquals(BetStatus.WON, bet.status)
        assertEquals(Money.euros(10), bet.profit)
        assertEquals(fixture.clock.instant(), bet.settledAt)
    }

    @Test
    fun `a bet cannot be settled twice`() {
        val bet = fixture.placeAndSettle(fixture.newBet(bankrollId), BetStatus.LOST)

        assertFailsWith<ConflictException> { bets.settle(bet.id, Settlement(BetStatus.WON, null)) }
    }

    @Test
    fun `an open bet can change its stake, odds and bookmaker`() {
        val bet = bets.create(fixture.newBet(bankrollId))

        val updated = bets.update(bet.id, BetChange(Money.euros(15), 2.5, "Unibet"))

        assertEquals(Money.euros(15), updated.stake)
        assertEquals(2.5, updated.odds)
        assertEquals("Unibet", updated.bookmaker)
    }

    @Test
    fun `a settled bet cannot be changed`() {
        val bet = fixture.placeAndSettle(fixture.newBet(bankrollId), BetStatus.WON)

        assertFailsWith<ConflictException> { bets.update(bet.id, BetChange(Money.euros(15), 2.5, "Unibet")) }
    }

    @Test
    fun `the stake of a palier is fixed by its montante`() {
        val montante = fixture.montantes.create(fixture.montanteConfig(bankrollId))
        val palier = bets.create(fixture.newBet(bankrollId, montanteId = montante.id))

        assertFailsWith<ConflictException> { bets.update(palier.id, BetChange(Money.euros(15), 1.8, "Winamax")) }
        assertEquals(1.8, bets.update(palier.id, BetChange(Money.euros(160), 1.8, "Winamax")).odds)
    }

    @Test
    fun `a montante bet stakes the whole capital on the montante bankroll`() {
        val montante = fixture.montantes.create(fixture.montanteConfig(bankrollId))

        val bet = bets.create(fixture.newBet(bankrollId, stake = 5, odds = 1.75, montanteId = montante.id))

        assertEquals(Money.euros(160), bet.stake)
        assertEquals(1, bet.palierNumber)
        assertEquals("Montante", bet.montanteName)
    }

    @Test
    fun `a montante accepts one pending palier at a time`() {
        val montante = fixture.montantes.create(fixture.montanteConfig(bankrollId))
        bets.create(fixture.newBet(bankrollId, montanteId = montante.id))

        assertFailsWith<ConflictException> { bets.create(fixture.newBet(bankrollId, montanteId = montante.id)) }
    }

    @Test
    fun `a finished montante accepts no more bets`() {
        val montante = fixture.montantes.create(fixture.montanteConfig(bankrollId))
        fixture.montantes.close(montante.id)

        assertFailsWith<ConflictException> { bets.create(fixture.newBet(bankrollId, montanteId = montante.id)) }
    }

    @Test
    fun `a direct bet can be deleted`() {
        val bet = fixture.placeAndSettle(fixture.newBet(bankrollId), BetStatus.WON)

        bets.delete(bet.id)

        assertFailsWith<NotFoundException> { bets.get(bet.id) }
    }

    @Test
    fun `a settled palier cannot be deleted`() {
        val montante = fixture.montantes.create(fixture.montanteConfig(bankrollId, relancesAllowed = 1))
        val palier = fixture.placeAndSettle(fixture.newBet(bankrollId, montanteId = montante.id), BetStatus.LOST)

        assertFailsWith<ConflictException> { bets.delete(palier.id) }
    }

    @Test
    fun `bets are filtered by status, bankroll, sport and bookmaker`() {
        val other = fixture.bankroll()
        val won = fixture.placeAndSettle(fixture.newBet(bankrollId, sport = "Tennis", bookmaker = "Unibet"), BetStatus.WON)
        val open = bets.create(fixture.newBet(other))

        assertEquals(listOf(won.id), ids(BetFilter(status = BetStatus.WON)))
        assertEquals(listOf(open.id), ids(BetFilter(bankrollId = other)))
        assertEquals(listOf(won.id), ids(BetFilter(sport = "tennis")))
        assertEquals(listOf(won.id), ids(BetFilter(bookmaker = "UNIBET")))
    }

    @Test
    fun `bets are filtered by period and free text`() {
        val old = bets.create(fixture.newBet(bankrollId, startsInSeconds = -40L * 86_400, bookmaker = "Betclic"))
        val recent = bets.create(fixture.newBet(bankrollId))

        assertEquals(listOf(recent.id), ids(BetFilter(lastDays = 30)))
        assertEquals(listOf(old.id), ids(BetFilter(query = " betclic ")))
        assertEquals(listOf(recent.id, old.id), ids(BetFilter(query = "lyon")))
    }

    @Test
    fun `a page of bets skips then limits the sorted list`() {
        val ids = (1..4).map { bets.create(fixture.newBet(bankrollId, startsInSeconds = it * 60L)).id }

        assertEquals(listOf(ids[2], ids[1]), ids(BetFilter(page = PageRequest(limit = 2, offset = 1))))
    }

    @Test
    fun `kelly only learns from decided bets in the same odds range`() {
        repeat(20) { fixture.placeAndSettle(fixture.newBet(bankrollId, odds = 3.5), BetStatus.WON) }

        assertEquals(Money.ZERO, bets.kelly(bankrollId, 2.0).stake)
    }

    @Test
    fun `kelly advises nothing without an edge`() {
        assertEquals(Money.ZERO, bets.kelly(bankrollId, 2.0).stake)
    }

    @Test
    fun `kelly advises a stake after a winning record`() {
        repeat(20) { fixture.placeAndSettle(fixture.newBet(bankrollId, odds = 1.8), BetStatus.WON) }

        assertTrue(bets.kelly(bankrollId, 1.8).stake.isPositive)
    }

    @Test
    fun `kelly rejects odds below 1`() {
        assertFailsWith<IllegalArgumentException> { bets.kelly(bankrollId, 1.0) }
    }
}
