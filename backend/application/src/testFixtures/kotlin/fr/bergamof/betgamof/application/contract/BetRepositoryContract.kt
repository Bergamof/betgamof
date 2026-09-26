package fr.bergamof.betgamof.application.contract

import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.BetId
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteId
import fr.bergamof.betgamof.domain.Settlement
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/** What every [fr.bergamof.betgamof.application.port.outbound.BetRepository] must guarantee. */
abstract class BetRepositoryContract : PersistenceContract() {
    private val repository get() = ports.bets

    private val bankrollId: BankrollId by lazy { addBankroll().id }

    private val montanteId: MontanteId by lazy { ports.montantes.add(newMontante(bankrollId)).id }

    @Test
    fun `add assigns fresh distinct ids and keeps everything else`() {
        val input = newBet(bankrollId)

        val first = repository.add(input)
        val second = repository.add(input)

        assertNotEquals(BetId.NEW, first.id)
        assertNotEquals(first.id, second.id)
        assertEquals(input, first.copy(id = BetId.NEW))
        assertEquals(input, second.copy(id = BetId.NEW))
    }

    @Test
    fun `findById round-trips every field and the selections in order`() {
        val stored = repository.add(newBet(bankrollId))

        val found = repository.findById(stored.id)

        assertEquals(stored, found)
        assertNull(found?.selections?.get(1)?.eventId)
    }

    @Test
    fun `findById round-trips a palier and its montante link`() {
        val palier = repository.add(newBet(bankrollId, montanteId, BetType.SIMPLE, listOf(selection("Lens – Lyon"))))

        assertEquals(palier, repository.findById(palier.id))
        assertEquals(montanteId, repository.findById(palier.id)?.montanteId)
    }

    @Test
    fun `findById returns only the selections of the requested bet`() {
        val first = repository.add(newBet(bankrollId))
        val second = repository.add(newBet(bankrollId, selections = listOf(selection("Nice – Rennes"), selection("Lille – Brest"))))

        assertEquals(first, repository.findById(first.id))
        assertEquals(second, repository.findById(second.id))
    }

    @Test
    fun `every type and status is stored and read back`() {
        val stored =
            BetType.entries.map { repository.add(newBet(bankrollId, type = it, selections = selectionsFor(it))) } +
                BetStatus.entries.map {
                    repository.add(
                        newBet(bankrollId).copy(status = it, cashout = Money(cents = 100), settledAt = now),
                    )
                }

        assertEquals(stored, stored.map { repository.findById(it.id) })
    }

    @Test
    fun `findAll returns every bet with its selections in creation order`() {
        val stored =
            listOf(
                repository.add(newBet(bankrollId)),
                repository.add(newBet(bankrollId, montanteId, BetType.SIMPLE, listOf(selection("Lens – Lyon")))),
                repository.add(newBet(bankrollId, selections = listOf(selection("Nice – Rennes"), selection("Lille – Brest")))),
            )

        assertEquals(stored, repository.findAll())
    }

    @Test
    fun `findById of an unknown id returns null`() {
        repository.add(newBet(bankrollId))

        assertNull(repository.findById(BetId(Long.MAX_VALUE)))
    }

    @Test
    fun `save stores a settlement with its cash-out and date`() {
        val stored = repository.add(newBet(bankrollId))
        val settled = stored.settle(Settlement(BetStatus.CASHOUT, Money(cents = 7_250)), later(seconds = 7_200))

        repository.save(settled)

        assertEquals(settled, repository.findById(stored.id))
    }

    @Test
    fun `save stores an amended bet`() {
        val stored = repository.add(newBet(bankrollId))
        val amended = stored.amend(Money(cents = 4_000), 2.85, "Unibet")

        repository.save(amended)

        assertEquals(amended, repository.findById(stored.id))
        assertEquals(listOf(amended), repository.findAll())
    }

    @Test
    fun `save rewrites the selections`() {
        val stored = repository.add(newBet(bankrollId))
        val other = repository.add(newBet(bankrollId))
        val changed = stored.copy(selections = listOf(selection("Nice – Rennes", eventId = null), selection("Lens – Lyon", odds = 1.9)))

        repository.save(changed)

        assertEquals(changed, repository.findById(stored.id))
        assertEquals(other, repository.findById(other.id))
    }

    @Test
    fun `save of an aggregate that was never added fails`() {
        assertFailsWith<IllegalStateException> { repository.save(newBet(bankrollId).copy(id = BetId(Long.MAX_VALUE))) }
    }

    @Test
    fun `remove deletes only the given bet`() {
        val kept = repository.add(newBet(bankrollId))
        val removed = repository.add(newBet(bankrollId))

        repository.remove(removed.id)

        assertNull(repository.findById(removed.id))
        assertEquals(listOf(kept), repository.findAll())
    }

    @Test
    fun `ids are not reused after a remove`() {
        val removed = repository.add(newBet(bankrollId))
        repository.remove(removed.id)

        val next = repository.add(newBet(bankrollId))

        assertNotEquals(removed.id, next.id)
    }

    private fun selectionsFor(type: BetType) =
        if (type == BetType.SIMPLE) listOf(selection("Lens – Lyon")) else listOf(selection("Lens – Lyon"), selection("PSG – OM"))
}
