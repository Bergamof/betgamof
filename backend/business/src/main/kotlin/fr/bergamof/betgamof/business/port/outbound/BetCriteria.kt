package fr.bergamof.betgamof.business.port.outbound

import fr.bergamof.betgamof.business.domain.Bet
import fr.bergamof.betgamof.business.domain.BetStatus
import fr.bergamof.betgamof.business.domain.OddsRange
import fr.bergamof.betgamof.business.domain.PageRequest
import fr.bergamof.betgamof.business.domain.Period
import java.time.Instant

/**
 * Which bets to load, so that each use case only reads the bets it needs.
 * Every criterion is optional; [matches] and [BetOrder.comparator] define the expected semantics
 * that repository implementations translate into their own query language.
 */
data class BetCriteria(
    val bankrollId: Long? = null,
    /** Only bets of these montantes. */
    val montanteIds: Set<Long>? = null,
    /** Only bets placed directly on a bankroll, outside any montante. */
    val directOnly: Boolean = false,
    val status: BetStatus? = null,
    /** Only won or lost bets. */
    val decidedOnly: Boolean = false,
    /** Sport of the first selection, ignoring case. */
    val sport: String? = null,
    /** Bookmaker, ignoring case. */
    val bookmaker: String? = null,
    /** Free text found in a selection (event, pick, competition, sport) or the bookmaker, ignoring case. */
    val text: String? = null,
    val oddsRange: OddsRange? = null,
    val placed: Period? = null,
    val starts: Period? = null,
    val settled: Period? = null,
    val order: BetOrder = BetOrder.CHRONOLOGICAL,
    val page: PageRequest? = null,
) {
    fun matches(bet: Bet): Boolean = matchesOwner(bet) && matchesOutcome(bet) && matchesContent(bet) && matchesDates(bet)

    private fun matchesOwner(bet: Bet) =
        (bankrollId == null || bet.bankrollId == bankrollId) &&
            (montanteIds == null || bet.montanteId in montanteIds) &&
            (!directOnly || bet.montanteId == null)

    private fun matchesOutcome(bet: Bet) =
        (status == null || bet.status == status) &&
            (!decidedOnly || bet.isDecided) &&
            (oddsRange == null || OddsRange.of(bet.odds) == oddsRange)

    private fun matchesContent(bet: Bet) =
        (sport == null || bet.sport.equals(sport, ignoreCase = true)) &&
            (bookmaker == null || bet.bookmaker.equals(bookmaker, ignoreCase = true)) &&
            (text.isNullOrBlank() || containsText(bet, text.trim()))

    private fun matchesDates(bet: Bet) =
        (placed == null || bet.placedAt in placed) &&
            (starts == null || bet.startsAt in starts) &&
            (settled == null || bet.settledAt?.let { it in settled } == true)

    private fun containsText(
        bet: Bet,
        text: String,
    ) = (bet.selections.flatMap { listOf(it.eventName, it.pick, it.competition, it.sport) } + bet.bookmaker)
        .any { it.contains(text, ignoreCase = true) }
}

enum class BetOrder(
    val comparator: Comparator<Bet>,
) {
    /** By placement date, oldest first. */
    CHRONOLOGICAL(compareBy<Bet> { it.placedAt }.thenBy { it.id }),

    /** By event date, latest first: the order of the bet list. */
    LATEST_START(compareByDescending<Bet> { it.startsAt }.thenByDescending { it.id }),

    /** By settlement date, latest first; pending bets last. */
    LATEST_SETTLED(compareByDescending<Bet, Instant?>(nullsFirst()) { it.settledAt }.thenByDescending { it.id }),
}
