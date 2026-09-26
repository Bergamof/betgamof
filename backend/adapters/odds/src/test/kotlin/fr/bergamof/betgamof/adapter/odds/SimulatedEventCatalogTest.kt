package fr.bergamof.betgamof.adapter.odds

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertTrue

class SimulatedEventCatalogTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-11T08:00:00Z"), ZoneOffset.UTC)
    private val events = SimulatedEventCatalog(clock, ZoneId.of("Europe/Paris")).upcoming()

    @Test
    fun `events are upcoming and have popular markets`() {
        assertTrue(events.isNotEmpty())
        assertTrue(events.all { it.startsAt.isAfter(clock.instant()) })
        assertTrue(events.all { event -> event.markets.any { it.popular } })
    }

    @Test
    fun `every outcome has real odds and a bookmaker`() {
        val outcomes = events.flatMap { event -> event.markets.flatMap { it.outcomes } }

        assertTrue(outcomes.all { it.odds > 1.0 && it.bookmaker.isNotBlank() })
    }
}
