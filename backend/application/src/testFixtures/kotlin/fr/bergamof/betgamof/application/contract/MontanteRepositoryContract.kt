package fr.bergamof.betgamof.application.contract

import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.MontanteId
import fr.bergamof.betgamof.domain.MontanteMode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/** What every [fr.bergamof.betgamof.application.port.outbound.MontanteRepository] must guarantee. */
abstract class MontanteRepositoryContract : PersistenceContract() {
    private val repository get() = ports.montantes

    private val bankrollId: BankrollId by lazy { addBankroll().id }

    @Test
    fun `add assigns fresh distinct ids and keeps everything else`() {
        val input = newMontante(bankrollId)

        val first = repository.add(input)
        val second = repository.add(input)

        assertNotEquals(MontanteId.NEW, first.id)
        assertNotEquals(first.id, second.id)
        assertEquals(input, first.copy(id = MontanteId.NEW))
        assertEquals(input, second.copy(id = MontanteId.NEW))
    }

    @Test
    fun `findById round-trips every field in every mode`() {
        val stored = MontanteMode.entries.map { repository.add(newMontante(bankrollId, it)) }

        assertEquals(stored, stored.map { repository.findById(it.id) })
    }

    @Test
    fun `findById round-trips a closed montante`() {
        val stored = repository.add(newMontante(bankrollId).copy(closedAt = later(seconds = 60)))

        assertEquals(stored, repository.findById(stored.id))
    }

    @Test
    fun `findAll returns every montante in creation order`() {
        val otherBankroll = addBankroll("Seconde").id
        val stored =
            listOf(
                repository.add(newMontante(bankrollId, MontanteMode.STEPS)),
                repository.add(newMontante(otherBankroll, MontanteMode.FREE)),
                repository.add(newMontante(bankrollId)),
            )

        assertEquals(stored, repository.findAll())
    }

    @Test
    fun `findById of an unknown id returns null`() {
        repository.add(newMontante(bankrollId))

        assertNull(repository.findById(MontanteId(Long.MAX_VALUE)))
    }

    @Test
    fun `save stores a closed montante`() {
        val stored = repository.add(newMontante(bankrollId))
        val closed = stored.copy(closedAt = later(seconds = 86_400))

        repository.save(closed)

        assertEquals(closed, repository.findById(stored.id))
        assertEquals(listOf(closed), repository.findAll())
    }

    @Test
    fun `save of an aggregate that was never added fails`() {
        assertFailsWith<IllegalStateException> { repository.save(newMontante(bankrollId).copy(id = MontanteId(Long.MAX_VALUE))) }
    }
}
