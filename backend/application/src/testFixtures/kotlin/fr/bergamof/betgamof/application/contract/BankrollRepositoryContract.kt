package fr.bergamof.betgamof.application.contract

import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.BankrollSettings
import fr.bergamof.betgamof.domain.Money
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/** What every [fr.bergamof.betgamof.application.port.outbound.BankrollRepository] must guarantee. */
abstract class BankrollRepositoryContract : PersistenceContract() {
    private val repository get() = ports.bankrolls

    @Test
    fun `add assigns fresh distinct ids and keeps everything else`() {
        val input = newBankroll()

        val first = repository.add(input)
        val second = repository.add(input)

        assertNotEquals(BankrollId.NEW, first.id)
        assertNotEquals(first.id, second.id)
        assertEquals(input, first.copy(id = BankrollId.NEW))
        assertEquals(input, second.copy(id = BankrollId.NEW))
    }

    @Test
    fun `findById round-trips every field`() {
        val stored = repository.add(newBankroll())

        assertEquals(stored, repository.findById(stored.id))
    }

    @Test
    fun `findById round-trips absent optional settings`() {
        val bare = BankrollSettings.of(name = "Sans limite", color = BankrollColor.BRIQUE, initialBalance = Money.ZERO)
        val stored = repository.add(newBankroll(bare))

        assertEquals(stored, repository.findById(stored.id))
    }

    @Test
    fun `every colour is stored and read back`() {
        val stored = BankrollColor.entries.map { repository.add(newBankroll(settings(name = it.toString(), color = it))) }

        assertEquals(stored, stored.map { repository.findById(it.id) })
    }

    @Test
    fun `findAll returns every bankroll in creation order`() {
        val stored = listOf(addBankroll("A"), addBankroll("B"), addBankroll("C"))

        assertEquals(stored, repository.findAll())
    }

    @Test
    fun `findById of an unknown id returns null`() {
        addBankroll()

        assertNull(repository.findById(BankrollId(Long.MAX_VALUE)))
    }

    @Test
    fun `save replaces the aggregate`() {
        val stored = addBankroll()
        val reconfigured =
            stored.reconfigure(
                BankrollSettings.of(name = "Renommée", color = BankrollColor.ORANGE, initialBalance = Money(cents = 5_000)),
            )

        repository.save(reconfigured)

        assertEquals(reconfigured, repository.findById(stored.id))
        assertEquals(listOf(reconfigured), repository.findAll())
    }

    @Test
    fun `save of an aggregate that was never added fails`() {
        assertFailsWith<IllegalStateException> { repository.save(newBankroll().copy(id = BankrollId(Long.MAX_VALUE))) }
    }

    @Test
    fun `remove deletes only the given bankroll`() {
        val kept = addBankroll("Gardée")
        val removed = addBankroll("Supprimée")

        repository.remove(removed.id)

        assertNull(repository.findById(removed.id))
        assertEquals(listOf(kept), repository.findAll())
    }

    @Test
    fun `ids are not reused after a remove`() {
        val removed = addBankroll()
        repository.remove(removed.id)

        val next = addBankroll()

        assertNotEquals(removed.id, next.id)
    }
}
