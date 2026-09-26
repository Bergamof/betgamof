package fr.bergamof.betgamof.domain

import kotlin.math.floor

/** Weight of the bookmaker's implied probability against the bettor's own record. */
private const val PRIOR_WEIGHT = 10.0
private const val MAX_PROBABILITY = 0.95

data class KellyAdvice(
    val stake: Money,
    val bankrollShare: Double,
    val probability: Double,
)

/**
 * Fractional Kelly stake. The win probability starts from the implied probability (1 / odds)
 * and is corrected by the bettor's calibration on past bets in the same odds range.
 */
object KellyAdvisor {
    fun advise(
        odds: Double,
        balance: Money,
        kellyFraction: Double,
        history: List<Bet>,
    ): KellyAdvice {
        ensureValid(odds > 1.0) { "La cote doit être supérieure à 1" }
        val probability = estimateProbability(odds, history)
        val netOdds = odds - 1
        val fullKelly = (netOdds * probability - (1 - probability)) / netOdds
        val share = (fullKelly * kellyFraction).coerceAtLeast(0.0)
        val stake = if (balance.isPositive) Money.euros(floor(balance.toEuros() * share)) else Money.ZERO
        return KellyAdvice(stake, if (balance.isPositive) stake / balance else 0.0, probability)
    }

    fun estimateProbability(
        odds: Double,
        history: List<Bet>,
    ): Double {
        val range = OddsRange.of(odds)
        val comparable = history.filter { it.isDecided && OddsRange.of(it.odds) == range }
        val wins = comparable.count { it.status == BetStatus.WON }
        val expectedWins = comparable.sumOf { 1 / it.odds }
        val calibration = (wins + PRIOR_WEIGHT) / (expectedWins + PRIOR_WEIGHT)
        return (calibration / odds).coerceAtMost(MAX_PROBABILITY)
    }
}

/** Odds bands used to calibrate the bettor and to break statistics down. [max] is exclusive; null means open-ended. */
enum class OddsRange(
    val min: Double,
    val max: Double?,
) {
    SAFE(1.0, 1.5),
    MEDIUM(1.5, 2.0),
    VALUE(2.0, 3.0),
    LONG_SHOT(3.0, null),
    ;

    companion object {
        fun of(odds: Double) = entries.first { it.max == null || odds < it.max }
    }
}
