package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.model.KellyView
import fr.bergamof.betgamof.application.model.MontanteSummaryView
import fr.bergamof.betgamof.application.model.ProfitPointView
import fr.bergamof.betgamof.application.model.RuleView
import fr.bergamof.betgamof.application.model.SegmentView
import fr.bergamof.betgamof.application.model.StatsView
import fr.bergamof.betgamof.application.model.StreakView
import fr.bergamof.betgamof.domain.RuleKind
import kotlinx.serialization.Serializable

@Serializable
data class RuleJson(
    val id: Long,
    val kind: RuleKind,
    val param: Int,
    val respected: Boolean,
)

fun RuleView.toJson() =
    RuleJson(
        id = id,
        kind = kind,
        param = param,
        respected = respected,
    )

@Serializable
data class KellyJson(
    val stake: Double,
    val bankrollShare: Double,
    val probability: Double,
)

fun KellyView.toJson() =
    KellyJson(
        stake = stake.toEuros(),
        bankrollShare = bankrollShare,
        probability = probability,
    )

@Serializable
data class SegmentJson(
    val label: String,
    val count: Int,
    val staked: Double,
    val profit: Double,
    val yield: Double,
)

fun SegmentView.toJson() =
    SegmentJson(
        label = label,
        count = count,
        staked = staked.toEuros(),
        profit = profit.toEuros(),
        yield = yield,
    )

@Serializable
data class ProfitPointJson(
    val at: JsonInstant,
    val cumulativeProfit: Double,
)

fun ProfitPointView.toJson() =
    ProfitPointJson(
        at = at,
        cumulativeProfit = cumulativeProfit.toEuros(),
    )

@Serializable
data class StreakJson(
    val won: Boolean,
    val length: Int,
)

fun StreakView.toJson() =
    StreakJson(
        won = won,
        length = length,
    )

@Serializable
data class MontanteSummaryJson(
    val launched: Int,
    val succeeded: Int,
    val broken: Int,
    val active: Int,
    val closed: Int,
    val averagePaliersReached: Double,
    val securedCoverage: Double?,
)

fun MontanteSummaryView.toJson() =
    MontanteSummaryJson(
        launched = launched,
        succeeded = succeeded,
        broken = broken,
        active = active,
        closed = closed,
        averagePaliersReached = averagePaliersReached,
        securedCoverage = securedCoverage,
    )

@Serializable
data class StatsJson(
    val settledCount: Int,
    val openCount: Int,
    val staked: Double,
    val profit: Double,
    val yield: Double,
    val won: Int,
    val lost: Int,
    val hitRate: Double,
    val averageOdds: Double,
    val breakEvenOdds: Double?,
    val averageStake: Double,
    val averageStakeShare: Double,
    val bestWinStreak: Int,
    val currentStreak: StreakJson?,
    val maxDrawdown: Double,
    val firstBetAt: JsonInstant?,
    val profitCurve: List<ProfitPointJson>,
    val bySport: List<SegmentJson>,
    val byOddsRange: List<SegmentJson>,
    val byMarket: List<SegmentJson>,
    val lateNight: SegmentJson,
    val placedDayBefore: SegmentJson,
    val montantes: MontanteSummaryJson,
)

fun StatsView.toJson() =
    StatsJson(
        settledCount = settledCount,
        openCount = openCount,
        staked = staked.toEuros(),
        profit = profit.toEuros(),
        yield = yield,
        won = won,
        lost = lost,
        hitRate = hitRate,
        averageOdds = averageOdds,
        breakEvenOdds = breakEvenOdds,
        averageStake = averageStake.toEuros(),
        averageStakeShare = averageStakeShare,
        bestWinStreak = bestWinStreak,
        currentStreak = currentStreak?.toJson(),
        maxDrawdown = maxDrawdown.toEuros(),
        firstBetAt = firstBetAt,
        profitCurve = profitCurve.map { it.toJson() },
        bySport = bySport.map { it.toJson() },
        byOddsRange = byOddsRange.map { it.toJson() },
        byMarket = byMarket.map { it.toJson() },
        lateNight = lateNight.toJson(),
        placedDayBefore = placedDayBefore.toJson(),
        montantes = montantes.toJson(),
    )
