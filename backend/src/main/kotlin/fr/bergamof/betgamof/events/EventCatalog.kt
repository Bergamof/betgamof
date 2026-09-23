package fr.bergamof.betgamof.events

import fr.bergamof.betgamof.domain.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

enum class MarketCategory { RESULTAT, BUTS, HANDICAP, BUTEURS, MI_TEMPS }

@Serializable
data class Outcome(
    val pick: String,
    val odds: Double,
    val bookmaker: String,
)

@Serializable
data class Market(
    val id: String,
    val name: String,
    val category: MarketCategory,
    val popular: Boolean,
    val outcomes: List<Outcome>,
)

@Serializable
data class SportEvent(
    val id: String,
    val sport: String,
    val competition: String,
    val name: String,
    @Serializable(with = InstantSerializer::class) val startsAt: Instant,
    val markets: List<Market>,
)

/** Source of upcoming events and their odds. */
fun interface EventSource {
    fun upcoming(): List<SportEvent>
}

/**
 * Simulated catalog used until a real odds provider is plugged in.
 * Kick-off times are anchored to "today" so the demo always has events to pick.
 */
class SimulatedEventCatalog(
    private val clock: Clock,
    private val zone: ZoneId,
) : EventSource {
    override fun upcoming(): List<SportEvent> {
        val today = clock.instant().atZone(zone).toLocalDate()

        fun at(
            daysFromNow: Long,
            time: String,
        ) = today
            .plusDays(daysFromNow)
            .atTime(LocalTime.parse(time))
            .atZone(zone)
            .toInstant()

        return listOf(
            football("lens-lyon", "Ligue 1", "Lens", "Lyon", at(0, "21:00"), FootballOdds(2.35, 3.40, 2.80, 1.72, 2.05, 1.66)),
            football("om-monaco", "Ligue 1", "Marseille", "Monaco", at(1, "17:00"), FootballOdds(1.95, 3.60, 3.70, 1.30, 1.88, 1.62)),
            football("psg-rennes", "Ligue 1", "PSG", "Rennes", at(2, "20:45"), FootballOdds(1.38, 5.00, 7.50, 1.22, 1.55, 1.80)),
            football("real-sevilla", "Liga", "Real Madrid", "Séville", at(1, "21:00"), FootballOdds(1.45, 4.60, 6.50, 1.25, 1.65, 1.85)),
            tennis("humbert-moutet", "ATP Metz", "Humbert", "Moutet", at(0, "19:30"), 2.10, 1.75),
            tennis("fils-mpetshi", "ATP Metz", "Fils", "Mpetshi Perricard", at(1, "14:00"), 1.62, 2.30),
            basket("asvel-monaco", "Euroligue", "ASVEL", "Monaco", at(0, "20:00"), 2.45, 1.55, 158.5),
            rugby("toulouse-rochelle", "Top 14", "Toulouse", "La Rochelle", at(2, "21:05"), 1.48, 2.70, 45.5),
        )
    }

    private data class FootballOdds(
        val home: Double,
        val draw: Double,
        val away: Double,
        val over15: Double,
        val over25: Double,
        val bothScore: Double,
    )

    private fun football(
        id: String,
        competition: String,
        home: String,
        away: String,
        startsAt: Instant,
        odds: FootballOdds,
    ) = SportEvent(
        id = id,
        sport = "Football",
        competition = competition,
        name = "$home – $away",
        startsAt = startsAt,
        markets =
            listOf(
                market(
                    "$id-1n2",
                    "Résultat final",
                    MarketCategory.RESULTAT,
                    true,
                    "Winamax",
                    home to odds.home,
                    "Match nul" to odds.draw,
                    away to odds.away,
                ),
                market(
                    "$id-total",
                    "Total de buts",
                    MarketCategory.BUTS,
                    true,
                    "Winamax",
                    "Plus de 1,5" to odds.over15,
                    "Plus de 2,5" to odds.over25,
                    "Moins de 2,5" to complement(odds.over25),
                ),
                market(
                    "$id-btts",
                    "Les deux équipes marquent",
                    MarketCategory.BUTS,
                    true,
                    "Betclic",
                    "Oui" to odds.bothScore,
                    "Non" to complement(odds.bothScore),
                ),
                market(
                    "$id-dc",
                    "Double chance",
                    MarketCategory.RESULTAT,
                    false,
                    "Unibet",
                    "$home ou nul" to doubleChance(odds.home, odds.draw),
                    "$away ou nul" to doubleChance(odds.away, odds.draw),
                ),
                market(
                    "$id-hcp",
                    "Handicap (-1)",
                    MarketCategory.HANDICAP,
                    false,
                    "Betclic",
                    "$home -1" to round(odds.home * HANDICAP_FACTOR),
                    "$away +1" to complement(odds.home * HANDICAP_FACTOR),
                ),
                market(
                    "$id-scorer",
                    "Buteur",
                    MarketCategory.BUTEURS,
                    false,
                    "Unibet",
                    "Un joueur de $home marque" to round(odds.home * SCORER_FACTOR),
                    "Un joueur de $away marque" to round(odds.away * SCORER_FACTOR),
                ),
                market(
                    "$id-ht",
                    "Résultat à la mi-temps",
                    MarketCategory.MI_TEMPS,
                    false,
                    "Winamax",
                    home to round(odds.home * HALF_TIME_FACTOR),
                    "Nul" to HALF_TIME_DRAW,
                    away to round(odds.away * HALF_TIME_FACTOR),
                ),
            ),
    )

    private fun tennis(
        id: String,
        competition: String,
        first: String,
        second: String,
        startsAt: Instant,
        firstOdds: Double,
        secondOdds: Double,
    ) = SportEvent(
        id = id,
        sport = "Tennis",
        competition = competition,
        name = "$first – $second",
        startsAt = startsAt,
        markets =
            listOf(
                market(
                    "$id-winner",
                    "Vainqueur",
                    MarketCategory.RESULTAT,
                    true,
                    "Betclic",
                    "$first gagne" to firstOdds,
                    "$second gagne" to secondOdds,
                ),
                market(
                    "$id-games",
                    "Total de jeux",
                    MarketCategory.BUTS,
                    true,
                    "Winamax",
                    "Plus de 21,5 jeux" to 1.80,
                    "Moins de 21,5 jeux" to 1.95,
                ),
                market(
                    "$id-set1",
                    "Vainqueur du 1er set",
                    MarketCategory.MI_TEMPS,
                    false,
                    "Unibet",
                    first to round(firstOdds * SET_FACTOR),
                    second to round(secondOdds * SET_FACTOR),
                ),
                market(
                    "$id-hcp",
                    "Handicap jeux (-2,5)",
                    MarketCategory.HANDICAP,
                    false,
                    "Betclic",
                    "$first -2,5" to round(firstOdds * HANDICAP_FACTOR),
                    "$second +2,5" to 1.70,
                ),
            ),
    )

    private fun basket(
        id: String,
        competition: String,
        home: String,
        away: String,
        startsAt: Instant,
        homeOdds: Double,
        awayOdds: Double,
        totalLine: Double,
    ) = SportEvent(
        id = id,
        sport = "Basket",
        competition = competition,
        name = "$home – $away",
        startsAt = startsAt,
        markets =
            listOf(
                market(
                    "$id-winner",
                    "Vainqueur (prolongations incluses)",
                    MarketCategory.RESULTAT,
                    true,
                    "Unibet",
                    home to homeOdds,
                    away to awayOdds,
                ),
                market(
                    "$id-total",
                    "Total de points",
                    MarketCategory.BUTS,
                    true,
                    "Winamax",
                    "Plus de ${line(totalLine)}" to 1.87,
                    "Moins de ${line(totalLine)}" to 1.87,
                ),
                market("$id-hcp", "Handicap (+5,5)", MarketCategory.HANDICAP, false, "Betclic", "$home +5,5" to 1.75, "$away -5,5" to 2.00),
            ),
    )

    private fun rugby(
        id: String,
        competition: String,
        home: String,
        away: String,
        startsAt: Instant,
        homeOdds: Double,
        awayOdds: Double,
        totalLine: Double,
    ) = SportEvent(
        id = id,
        sport = "Rugby",
        competition = competition,
        name = "$home – $away",
        startsAt = startsAt,
        markets =
            listOf(
                market("$id-winner", "Vainqueur", MarketCategory.RESULTAT, true, "Winamax", home to homeOdds, away to awayOdds),
                market(
                    "$id-total",
                    "Total de points",
                    MarketCategory.BUTS,
                    true,
                    "Betclic",
                    "Plus de ${line(totalLine)}" to 1.85,
                    "Moins de ${line(totalLine)}" to 1.90,
                ),
                market("$id-hcp", "Handicap (-7,5)", MarketCategory.HANDICAP, false, "Unibet", "$home -7,5" to 2.05, "$away +7,5" to 1.72),
            ),
    )

    private fun market(
        id: String,
        name: String,
        category: MarketCategory,
        popular: Boolean,
        bookmaker: String,
        vararg outcomes: Pair<String, Double>,
    ) = Market(id, name, category, popular, outcomes.map { (pick, odds) -> Outcome(pick, odds, bookmaker) })

    private fun line(value: Double) = value.toString().replace('.', ',')

    private companion object {
        const val BOOKMAKER_MARGIN = 1.06
        const val HANDICAP_FACTOR = 1.9
        const val SCORER_FACTOR = 0.75
        const val HALF_TIME_FACTOR = 1.45
        const val HALF_TIME_DRAW = 2.20
        const val SET_FACTOR = 0.95
        const val MIN_ODDS = 1.01

        fun round(odds: Double) = (Math.round(odds * 100) / 100.0).coerceAtLeast(MIN_ODDS)

        /** Odds of the opposite outcome of a two-way market, keeping a bookmaker margin. */
        fun complement(odds: Double) = round(1 / (BOOKMAKER_MARGIN - 1 / odds))

        fun doubleChance(
            win: Double,
            draw: Double,
        ) = round(1 / (1 / win + 1 / draw) * BOOKMAKER_MARGIN)
    }
}
