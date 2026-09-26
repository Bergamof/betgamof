package fr.bergamof.betgamof.domain

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

private const val LATE_NIGHT_HOUR = 22

data class ProfitPoint(
    val at: Instant,
    val cumulativeProfit: Money,
)

data class Segment(
    val label: String,
    val count: Int,
    val staked: Money,
    val profit: Money,
) {
    val yield get() = if (staked.isPositive) profit / staked else 0.0
}

data class Streak(
    val won: Boolean,
    val length: Int,
)

data class MontanteSummary(
    val launched: Int,
    val succeeded: Int,
    val broken: Int,
    val active: Int,
    val closed: Int,
    val averagePaliersReached: Double,
    /** Share of montante losses covered by the gains secured along the way. */
    val securedCoverage: Double?,
)

data class Stats(
    val settledCount: Int,
    val openCount: Int,
    val staked: Money,
    val profit: Money,
    val won: Int,
    val lost: Int,
    val averageOdds: Double,
    val averageStake: Money,
    val bestWinStreak: Int,
    val currentStreak: Streak?,
    val maxDrawdown: Money,
    val firstBetAt: Instant?,
    val profitCurve: List<ProfitPoint>,
    val bySport: List<Segment>,
    val byOddsRange: List<Segment>,
    val byMarket: List<Segment>,
    val lateNight: Segment,
    val placedDayBefore: Segment,
    val montantes: MontanteSummary,
) {
    val yield get() = if (staked.isPositive) profit / staked else 0.0

    val hitRate get() = if (won + lost > 0) won.toDouble() / (won + lost) else 0.0

    val breakEvenOdds get() = if (hitRate > 0) 1 / hitRate else null
}

object StatsCalculator {
    fun compute(
        bets: List<Bet>,
        montantes: List<MontanteState>,
        zone: ZoneId,
    ): Stats {
        val settled = bets.filter { it.isSettled }.sortedBy { it.settledAt }
        val decided = settled.filter { it.isDecided }
        return Stats(
            settledCount = settled.size,
            openCount = bets.count { it.isOpen },
            staked = settled.map { it.stake }.sum(),
            profit = settled.map { it.profit }.sum(),
            won = decided.count { it.status == BetStatus.WON },
            lost = decided.count { it.status == BetStatus.LOST },
            averageOdds = if (settled.isEmpty()) 0.0 else settled.map { it.odds }.average(),
            averageStake = if (settled.isEmpty()) Money.ZERO else Money(settled.map { it.stake.cents }.average().toLong()),
            bestWinStreak = bestWinStreak(decided),
            currentStreak = currentStreak(decided),
            maxDrawdown = maxDrawdown(settled),
            firstBetAt = bets.minOfOrNull { it.placedAt },
            profitCurve = profitCurve(settled),
            bySport = segment(settled) { it.sport }.sortedByDescending { it.profit },
            byOddsRange = OddsRange.entries.map { range -> segmentOf(range.label, settled.filter { OddsRange.of(it.odds) == range }) },
            byMarket = segment(settled) { it.market }.sortedByDescending { it.profit },
            lateNight = segmentOf("late-night", settled.filter { it.placedAt.atZone(zone).hour >= LATE_NIGHT_HOUR }),
            placedDayBefore = segmentOf("day-before", settled.filter { isPlacedDayBefore(it, zone) }),
            montantes = summarize(montantes),
        )
    }

    fun bestWinStreak(decided: List<Bet>): Int {
        var best = 0
        var current = 0
        decided.forEach { bet ->
            current = if (bet.status == BetStatus.WON) current + 1 else 0
            best = maxOf(best, current)
        }
        return best
    }

    fun currentStreak(decided: List<Bet>): Streak? {
        val last = decided.lastOrNull() ?: return null
        val length = decided.asReversed().takeWhile { it.status == last.status }.size
        return Streak(last.status == BetStatus.WON, length)
    }

    fun maxDrawdown(settled: List<Bet>): Money {
        var cumulative = Money.ZERO
        var peak = Money.ZERO
        var worst = Money.ZERO
        settled.forEach { bet ->
            cumulative += bet.profit
            peak = maxOf(peak, cumulative)
            worst = maxOf(worst, peak - cumulative)
        }
        return worst
    }

    private fun profitCurve(settled: List<Bet>): List<ProfitPoint> {
        var cumulative = Money.ZERO
        return settled.map { bet ->
            cumulative += bet.profit
            ProfitPoint(requireNotNull(bet.settledAt), cumulative)
        }
    }

    private fun segment(
        bets: List<Bet>,
        key: (Bet) -> String,
    ) = bets.groupBy(key).map { (label, group) -> segmentOf(label, group) }

    private fun segmentOf(
        label: String,
        bets: List<Bet>,
    ) = Segment(label, bets.size, bets.map { it.stake }.sum(), bets.map { it.profit }.sum())

    private fun isPlacedDayBefore(
        bet: Bet,
        zone: ZoneId,
    ) = bet.placedAt
        .atZone(zone)
        .toLocalDate()
        .isBefore(bet.startsAt.atZone(zone).toLocalDate()) &&
        Duration.between(bet.placedAt, bet.startsAt).isPositive

    private fun summarize(states: List<MontanteState>): MontanteSummary {
        val finished = states.filter { !it.isActive }
        val losses = finished.filter { it.result < Money.ZERO }
        val lostCapital = losses.map { it.engaged - it.capital }.sum()
        return MontanteSummary(
            launched = states.size,
            succeeded = states.count { it.status == MontanteStatus.SUCCEEDED },
            broken = states.count { it.status == MontanteStatus.BROKEN },
            active = states.count { it.isActive },
            closed = states.count { it.status == MontanteStatus.CLOSED },
            averagePaliersReached =
                if (finished.isEmpty()) {
                    0.0
                } else {
                    finished
                        .map { state ->
                            state.paliers.count {
                                it.bet.status ==
                                    BetStatus.WON
                            }
                        }.average()
                },
            securedCoverage = if (lostCapital.isPositive) losses.map { it.secured }.sum() / lostCapital else null,
        )
    }
}
