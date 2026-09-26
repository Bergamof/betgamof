package fr.bergamof.betgamof.application.service

import fr.bergamof.betgamof.application.model.MarketView
import fr.bergamof.betgamof.application.model.OutcomeView
import fr.bergamof.betgamof.application.model.SportEventView
import fr.bergamof.betgamof.application.port.inbound.EventUseCases
import fr.bergamof.betgamof.application.port.outbound.EventCatalog
import fr.bergamof.betgamof.domain.SportEvent

class EventService(
    private val catalog: EventCatalog,
) : EventUseCases {
    override fun upcoming(query: String?): List<SportEventView> =
        catalog
            .upcoming()
            .filter { query.isNullOrBlank() || it.name.contains(query.trim(), ignoreCase = true) }
            .map { it.toView() }

    private fun SportEvent.toView() =
        SportEventView(
            id = id,
            sport = sport,
            competition = competition,
            name = name,
            startsAt = startsAt,
            markets =
                markets.map { market ->
                    MarketView(
                        market.id,
                        market.name,
                        market.category,
                        market.popular,
                        market.outcomes.map { OutcomeView(it.pick, it.odds, it.bookmaker) },
                    )
                },
        )
}
