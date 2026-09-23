package fr.bergamof.betgamof.application.model

import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteMode
import fr.bergamof.betgamof.domain.MontanteStatus
import fr.bergamof.betgamof.domain.RuleKind
import java.time.Instant

// Read models returned by the use cases. Adapters decide how to expose them (JSON for the REST API).

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

data class SelectionView(
    val eventId: String?,
    val eventName: String,
    val sport: String,
    val competition: String,
    val market: String,
    val pick: String,
    val odds: Double,
)

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
    val placedAt: Instant,
    val startsAt: Instant,
    val settledAt: Instant?,
)

data class PalierView(
    val number: Int,
    val betId: Long,
    val label: String,
    val bookmaker: String,
    val startsAt: Instant,
    val stake: Money,
    val odds: Double,
    val status: BetStatus,
    val secured: Money,
    val capitalAfter: Money,
    val isRelance: Boolean,
)

data class LossScenario(
    /** True when a relance is still available: the montante restarts from its starting capital. */
    val relance: Boolean,
    val capitalAfter: Money,
    val securedKept: Money,
    val lostAmount: Money,
)

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
    val createdAt: Instant,
    val closedAt: Instant?,
    val paliers: List<PalierView>,
    val nextStep: NextStepView?,
)

data class PlannedStepView(
    val number: Int,
    val stake: Money,
    val secured: Money,
    val capitalAfter: Money,
)

data class MontantePlanView(
    val target: Money?,
    val plannedSteps: Int?,
    val steps: List<PlannedStepView>,
    val successProbability: Double,
    val bankrollBalance: Money,
    val bankrollBalanceAfterLaunch: Money,
)

data class RuleView(
    val id: Long,
    val kind: RuleKind,
    val param: Int,
    val respected: Boolean,
)

data class KellyView(
    val stake: Money,
    val bankrollShare: Double,
    val probability: Double,
)

data class SegmentView(
    val label: String,
    val count: Int,
    val staked: Money,
    val profit: Money,
    val yield: Double,
)

data class ProfitPointView(
    val at: Instant,
    val cumulativeProfit: Money,
)

data class StreakView(
    val won: Boolean,
    val length: Int,
)

data class MontanteSummaryView(
    val launched: Int,
    val succeeded: Int,
    val broken: Int,
    val active: Int,
    val closed: Int,
    val averagePaliersReached: Double,
    val securedCoverage: Double?,
)

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
    val firstBetAt: Instant?,
    val profitCurve: List<ProfitPointView>,
    val bySport: List<SegmentView>,
    val byOddsRange: List<SegmentView>,
    val byMarket: List<SegmentView>,
    val lateNight: SegmentView,
    val placedDayBefore: SegmentView,
    val montantes: MontanteSummaryView,
)
