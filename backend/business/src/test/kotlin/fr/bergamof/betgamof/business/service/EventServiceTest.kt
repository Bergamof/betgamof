package fr.bergamof.betgamof.business.service

import fr.bergamof.betgamof.business.domain.SportEvent
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class EventServiceTest {
    private val events =
        listOf("PSG – OM", "Lens – Lyon").mapIndexed { index, name ->
            SportEvent("e$index", "Football", "Ligue 1", name, Instant.EPOCH, emptyList())
        }
    private val service = EventService { events }

    @Test
    fun `without a query every upcoming event is returned`() {
        assertEquals(events, service.upcoming(null))
        assertEquals(events, service.upcoming("  "))
    }

    @Test
    fun `a query filters events by name, ignoring case`() {
        assertEquals(listOf("Lens – Lyon"), service.upcoming(" lyon ").map { it.name })
    }
}
