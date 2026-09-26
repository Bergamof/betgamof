package fr.bergamof.betgamof.domain

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

private const val LATE_NIGHT_HOUR = 22

data class ProfitPoint(
    val at: Instant,
    val cumulativeProfit: Money,
)

/** What a statistics segment groups bets by. Adapters turn it into a label. */
sealed interface SegmentKey {
    data class Sport(
        val name: String,
    ) : SegmentKey

    data class Market(
        val name: String,
    ) : SegmentKey

    /** Combined and system bets, which span several markets. */
    data object MultipleMarkets : SegmentKey

    data class Odds(
        val range: OddsRange,
    ) : SegmentKey

    /** Bets placed at 22:00 or later. */
    data object LateNight : SegmentKey

    /** Bets placed the day before the event starts. */
    data object DayBefore : SegmentKey
}

data class Segment(
    val key: SegmentKey,
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
    /** Average stake as a share of the current balance. */
    val averageStakeShare: Double,
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
        balance: Money,
        zone: ZoneId,
    ): Stats {
        val settled = bets.filter { it.isSettled }.sortedBy { it.settledAt }
        val decided = settled.filter { it.isDecided }
        val averageStake = if (settled.isEmpty()) Money.ZERO else Money(settled.map { it.stake.cents }.average().toLong())
        return Stats(
            settledCount = settled.size,
            openCount = bets.count { it.isOpen },
            staked = settled.map { it.stake }.sum(),
            profit = settled.map { it.profit }.sum(),
            won = decided.count { it.status == BetStatus.WON },
            lost = decided.count { it.status == BetStatus.LOST },
            averageOdds = if (settled.isEmpty()) 0.0 else settled.map { it.odds }.average(),
            averageStake = averageStake,
            averageStakeShare = if (balance.isPositive) averageStake / balance else 0.0,
            bestWinStreak = bestWinStreak(decided),
            currentStreak = currentStreak(decided),
            maxDrawdown = maxDrawdown(settled),
            firstBetAt = bets.minOfOrNull { it.placedAt },
            profitCurve = profitCurve(settled),
            bySport = segment(settled) { SegmentKey.Sport(it.sport) }.sortedByDescending { it.profit },
            byOddsRange =
                OddsRange.entries.map { range -> segmentOf(SegmentKey.Odds(range), settled.filter { OddsRange.of(it.odds) == range }) },
            byMarket =
                segment(
                    settled,
                ) { bet -> bet.market?.let(SegmentKey::Market) ?: SegmentKey.MultipleMarkets }.sortedByDescending { it.profit },
            lateNight = segmentOf(SegmentKey.LateNight, settled.filter { it.placedAt.atZone(zone).hour >= LATE_NIGHT_HOUR }),
            placedDayBefore = segmentOf(SegmentKey.DayBefore, settled.filter { isPlacedDayBefore(it, zone) }),
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
        key: (Bet) -> SegmentKey,
    ) = bets.groupBy(key).map { (segmentKey, group) -> segmentOf(segmentKey, group) }

    private fun segmentOf(
        key: SegmentKey,
        bets: List<Bet>,
    ) = Segment(key, bets.size, bets.map { it.stake }.sum(), bets.map { it.profit }.sum())

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
