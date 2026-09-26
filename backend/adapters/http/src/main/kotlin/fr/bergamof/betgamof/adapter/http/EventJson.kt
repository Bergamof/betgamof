package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.model.MarketView
import fr.bergamof.betgamof.application.model.OutcomeView
import fr.bergamof.betgamof.application.model.SportEventView
import kotlinx.serialization.Serializable

@Serializable
data class OutcomeJson(
    val pick: String,
    val odds: Double,
    val bookmaker: String,
)

fun OutcomeView.toJson() = OutcomeJson(pick, odds, bookmaker)

@Serializable
data class MarketJson(
    val id: String,
    val name: String,
    val category: MarketCategoryJson,
    val popular: Boolean,
    val outcomes: List<OutcomeJson>,
)

fun MarketView.toJson() = MarketJson(id, name, category.toJson(), popular, outcomes.map { it.toJson() })

@Serializable
data class SportEventJson(
    val id: String,
    val sport: String,
    val competition: String,
    val name: String,
    val startsAt: JsonInstant,
    val markets: List<MarketJson>,
)

fun SportEventView.toJson() = SportEventJson(id, sport, competition, name, startsAt, markets.map { it.toJson() })
