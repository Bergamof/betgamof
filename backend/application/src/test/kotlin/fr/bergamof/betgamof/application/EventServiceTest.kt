package fr.bergamof.betgamof.application

import fr.bergamof.betgamof.application.model.MarketView
import fr.bergamof.betgamof.application.model.OutcomeView
import fr.bergamof.betgamof.application.model.SportEventView
import fr.bergamof.betgamof.application.port.outbound.EventCatalog
import fr.bergamof.betgamof.application.service.EventService
import fr.bergamof.betgamof.domain.Market
import fr.bergamof.betgamof.domain.MarketCategory
import fr.bergamof.betgamof.domain.Outcome
import fr.bergamof.betgamof.domain.SportEvent
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class EventServiceTest {
    private val startsAt = Instant.parse("2026-09-11T19:00:00Z")
    private val lensLyon =
        SportEvent(
            id = "lens-lyon",
            sport = "Football",
            competition = "Ligue 1",
            name = "Lens – Lyon",
            startsAt = startsAt,
            markets =
                listOf(
                    Market(
                        "lens-lyon-goals",
                        "Total de buts",
                        MarketCategory.BUTS,
                        true,
                        listOf(Outcome("Plus de 1,5", 1.72, "Winamax"), Outcome("Moins de 1,5", 2.05, "Betclic")),
                    ),
                ),
        )
    private val humbertMoutet = SportEvent("humbert-moutet", "Tennis", "ATP Metz", "Humbert – Moutet", startsAt, emptyList())
    private val events = EventService(EventCatalog { listOf(lensLyon, humbertMoutet) })

    @Test
    fun `without query every upcoming event is listed`() {
        assertEquals(listOf("lens-lyon", "humbert-moutet"), events.upcoming(null).map { it.id })
        assertEquals(listOf("lens-lyon", "humbert-moutet"), events.upcoming("  ").map { it.id })
    }

    @Test
    fun `the query matches event names ignoring case and surrounding blanks`() {
        assertEquals(listOf("humbert-moutet"), events.upcoming(" moutet ").map { it.id })
        assertEquals(listOf("lens-lyon"), events.upcoming("LENS").map { it.id })
        assertEquals(emptyList(), events.upcoming("Ligue 1"))
    }

    @Test
    fun `events are copied into views with their markets and outcomes`() {
        val expected =
            SportEventView(
                id = "lens-lyon",
                sport = "Football",
                competition = "Ligue 1",
                name = "Lens – Lyon",
                startsAt = startsAt,
                markets =
                    listOf(
                        MarketView(
                            "lens-lyon-goals",
                            "Total de buts",
                            MarketCategory.BUTS,
                            true,
                            listOf(OutcomeView("Plus de 1,5", 1.72, "Winamax"), OutcomeView("Moins de 1,5", 2.05, "Betclic")),
                        ),
                    ),
            )

        assertEquals(expected, events.upcoming("lens").single())
    }
}
