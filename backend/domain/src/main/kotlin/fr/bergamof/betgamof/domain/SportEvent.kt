package fr.bergamof.betgamof.domain

import java.time.Instant

enum class MarketCategory { RESULTAT, BUTS, HANDICAP, BUTEURS, MI_TEMPS }

/** One possible result of a market, with the best odds found and the bookmaker offering them. */
data class Outcome(
    val pick: String,
    val odds: Double,
    val bookmaker: String,
)

data class Market(
    val id: String,
    val name: String,
    val category: MarketCategory,
    val popular: Boolean,
    val outcomes: List<Outcome>,
)

/** An upcoming match a bet can be built from. */
data class SportEvent(
    val id: String,
    val sport: String,
    val competition: String,
    val name: String,
    val startsAt: Instant,
    val markets: List<Market>,
)
