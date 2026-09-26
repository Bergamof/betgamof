package fr.bergamof.betgamof.application.contract

import fr.bergamof.betgamof.domain.DisciplineRule
import fr.bergamof.betgamof.domain.RuleId
import fr.bergamof.betgamof.domain.RuleKind
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/** What every [fr.bergamof.betgamof.application.port.outbound.RuleRepository] must guarantee. */
abstract class RuleRepositoryContract : PersistenceContract() {
    private val repository get() = ports.rules

    private val maxStake = DisciplineRule.define(RuleKind.MAX_STAKE_PCT, param = 3)

    @Test
    fun `add assigns fresh distinct ids and keeps everything else`() {
        val first = repository.add(maxStake)
        val second = repository.add(maxStake)

        assertNotEquals(RuleId.NEW, first.id)
        assertNotEquals(first.id, second.id)
        assertEquals(maxStake, first.copy(id = RuleId.NEW))
        assertEquals(maxStake, second.copy(id = RuleId.NEW))
    }

    @Test
    fun `every kind is stored and read back`() {
        val stored =
            listOf(
                repository.add(maxStake),
                repository.add(DisciplineRule.define(RuleKind.PAUSE_AFTER_LOSSES, param = 4)),
                repository.add(DisciplineRule.define(RuleKind.SINGLE_ACTIVE_MONTANTE, param = 0)),
            )

        assertEquals(RuleKind.entries.toSet(), stored.map { it.kind }.toSet())
        assertEquals(stored, stored.map { repository.findById(it.id) })
        assertEquals(stored, repository.findAll())
    }

    @Test
    fun `findById of an unknown id returns null`() {
        repository.add(maxStake)

        assertNull(repository.findById(RuleId(Long.MAX_VALUE)))
    }

    @Test
    fun `remove deletes only the given rule`() {
        val kept = repository.add(maxStake)
        val removed = repository.add(DisciplineRule.define(RuleKind.PAUSE_AFTER_LOSSES, param = 2))

        repository.remove(removed.id)

        assertNull(repository.findById(removed.id))
        assertEquals(listOf(kept), repository.findAll())
    }

    @Test
    fun `ids are not reused after a remove`() {
        val removed = repository.add(maxStake)
        repository.remove(removed.id)

        val next = repository.add(maxStake)

        assertNotEquals(removed.id, next.id)
    }
}
