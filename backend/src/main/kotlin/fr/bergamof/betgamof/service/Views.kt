package fr.bergamof.betgamof.service

import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.InstantSerializer
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteMode
import fr.bergamof.betgamof.domain.MontanteStatus
import fr.bergamof.betgamof.domain.RuleKind
import kotlinx.serialization.Serializable
import java.time.Instant

// Read models returned by the API. Amounts are euros, instants ISO-8601.

typealias IsoInstant =
    @Serializable(with = InstantSerializer::class)
    Instant

@Serializable
data class BankrollView(
    val id: Long,
    val name: String,
    val color: BankrollColor,
    val initialBalance: Money,
    val balance: Money,
    val stopLoss: Money?,
    val stopLossMargin: Money?,
    val kellyFraction: Double,
    val fixedStake: Money?,
    val outsideMontantes: Money,
    val openStake: Money,
    val staked: Money,
    val profit: Money,
    val roi: Double,
    val betCount: Int,
    val bookmakers: List<String>,
)

@Serializable
data class SelectionView(
    val eventId: String?,
    val eventName: String,
    val sport: String,
    val competition: String,
    val market: String,
    val pick: String,
    val odds: Double,
)

@Serializable
data class BetView(
    val id: Long,
    val bankrollId: Long,
    val bankrollName: String,
    val montanteId: Long?,
    val montanteName: String?,
    val palierNumber: Int?,
    val type: BetType,
    val bookmaker: String,
    val stake: Money,
    val odds: Double,
    val status: BetStatus,
    val cashout: Money?,
    val profit: Money,
    val potentialReturn: Money,
    val label: String,
    val sport: String,
    val competition: String,
    val market: String,
    val selections: List<SelectionView>,
    val placedAt: IsoInstant,
    val startsAt: IsoInstant,
    val settledAt: IsoInstant?,
)

@Serializable
data class PalierView(
    val number: Int,
    val betId: Long,
    val label: String,
    val bookmaker: String,
    val startsAt: IsoInstant,
    val stake: Money,
    val odds: Double,
    val status: BetStatus,
    val secured: Money,
    val capitalAfter: Money,
    val isRelance: Boolean,
)

@Serializable
data class LossScenario(
    /** True when a relance is still available: the montante restarts from its starting capital. */
    val relance: Boolean,
    val capitalAfter: Money,
    val securedKept: Money,
    val lostAmount: Money,
)

@Serializable
data class NextStepView(
    val number: Int,
    val stake: Money,
    val isRelance: Boolean,
    val requiredOdds: Double?,
    val openBet: BetView?,
    val capitalIfWon: Money?,
    val securedIfWon: Money?,
    val ifLost: LossScenario,
)

@Serializable
data class MontanteView(
    val id: Long,
    val name: String,
    val bankrollId: Long,
    val bankrollName: String,
    val mode: MontanteMode,
    val targetMultiplier: Double?,
    val stepCount: Int?,
    val targetOdds: Double,
    val excludeStake: Boolean,
    val securePct: Int,
    val relancesAllowed: Int,
    val relancesUsed: Int,
    val startCapital: Money,
    val status: MontanteStatus,
    val capital: Money,
    val engaged: Money,
    val secured: Money,
    val result: Money,
    val target: Money?,
    val plannedSteps: Int?,
    val currentPalier: Int,
    /** Paliers played plus those still planned; null in free mode. */
    val totalPaliers: Int?,
    val progress: Double?,
    val successProbability: Double?,
    val createdAt: IsoInstant,
    val closedAt: IsoInstant?,
    val paliers: List<PalierView>,
    val nextStep: NextStepView?,
)

@Serializable
data class PlannedStepView(
    val number: Int,
    val stake: Money,
    val secured: Money,
    val capitalAfter: Money,
)

@Serializable
data class MontantePlanView(
    val target: Money?,
    val plannedSteps: Int?,
    val steps: List<PlannedStepView>,
    val successProbability: Double,
    val bankrollBalance: Money,
    val bankrollBalanceAfterLaunch: Money,
)

@Serializable
data class RuleView(
    val id: Long,
    val kind: RuleKind,
    val param: Int,
    val respected: Boolean,
)

@Serializable
data class KellyView(
    val stake: Money,
    val bankrollShare: Double,
    val probability: Double,
)

@Serializable
data class SegmentView(
    val label: String,
    val count: Int,
    val staked: Money,
    val profit: Money,
    val yield: Double,
)

@Serializable
data class ProfitPointView(
    val at: IsoInstant,
    val cumulativeProfit: Money,
)

@Serializable
data class StreakView(
    val won: Boolean,
    val length: Int,
)

@Serializable
data class MontanteSummaryView(
    val launched: Int,
    val succeeded: Int,
    val broken: Int,
    val active: Int,
    val closed: Int,
    val averagePaliersReached: Double,
    val securedCoverage: Double?,
)

@Serializable
data class StatsView(
    val settledCount: Int,
    val openCount: Int,
    val staked: Money,
    val profit: Money,
    val yield: Double,
    val won: Int,
    val lost: Int,
    val hitRate: Double,
    val averageOdds: Double,
    val breakEvenOdds: Double?,
    val averageStake: Money,
    val averageStakeShare: Double,
    val bestWinStreak: Int,
    val currentStreak: StreakView?,
    val maxDrawdown: Money,
    val firstBetAt: IsoInstant?,
    val profitCurve: List<ProfitPointView>,
    val bySport: List<SegmentView>,
    val byOddsRange: List<SegmentView>,
    val byMarket: List<SegmentView>,
    val lateNight: SegmentView,
    val placedDayBefore: SegmentView,
    val montantes: MontanteSummaryView,
)
