package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.domain.Market
import fr.bergamof.betgamof.domain.MarketCategory
import fr.bergamof.betgamof.domain.SportEvent
import kotlinx.serialization.Serializable

@Serializable
data class OutcomeJson(
    val pick: String,
    val odds: Double,
    val bookmaker: String,
)

@Serializable
data class MarketJson(
    val id: String,
    val name: String,
    val category: MarketCategory,
    val popular: Boolean,
    val outcomes: List<OutcomeJson>,
)

fun Market.toJson() = MarketJson(id, name, category, popular, outcomes.map { OutcomeJson(it.pick, it.odds, it.bookmaker) })

@Serializable
data class SportEventJson(
    val id: String,
    val sport: String,
    val competition: String,
    val name: String,
    val startsAt: JsonInstant,
    val markets: List<MarketJson>,
)

fun SportEvent.toJson() = SportEventJson(id, sport, competition, name, startsAt, markets.map { it.toJson() })
