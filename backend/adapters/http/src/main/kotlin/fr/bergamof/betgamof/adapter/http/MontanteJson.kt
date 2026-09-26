package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.model.LossScenarioView
import fr.bergamof.betgamof.application.model.MontantePlanView
import fr.bergamof.betgamof.application.model.MontanteView
import fr.bergamof.betgamof.application.model.NextStepView
import fr.bergamof.betgamof.application.model.PalierView
import fr.bergamof.betgamof.application.model.PlannedStepView
import kotlinx.serialization.Serializable

@Serializable
data class PalierJson(
    val number: Int,
    val betId: Long,
    val label: String,
    val bookmaker: String,
    val startsAt: JsonInstant,
    val stake: Double,
    val odds: Double,
    val status: BetStatusJson,
    val secured: Double,
    val capitalAfter: Double,
    val isRelance: Boolean,
)

fun PalierView.toJson() =
    PalierJson(
        number = number,
        betId = betId.value,
        label = betLabel(type, selections),
        bookmaker = bookmaker,
        startsAt = startsAt,
        stake = stake.toEuros(),
        odds = odds,
        status = status.toJson(),
        secured = secured.toEuros(),
        capitalAfter = capitalAfter.toEuros(),
        isRelance = isRelance,
    )

@Serializable
data class LossScenarioJson(
    val relance: Boolean,
    val capitalAfter: Double,
    val securedKept: Double,
    val lostAmount: Double,
)

fun LossScenarioView.toJson() =
    LossScenarioJson(
        relance = relance,
        capitalAfter = capitalAfter.toEuros(),
        securedKept = securedKept.toEuros(),
        lostAmount = lostAmount.toEuros(),
    )

@Serializable
data class NextStepJson(
    val number: Int,
    val stake: Double,
    val isRelance: Boolean,
    val requiredOdds: Double?,
    val openBet: BetJson?,
    val capitalIfWon: Double?,
    val securedIfWon: Double?,
    val ifLost: LossScenarioJson,
)

fun NextStepView.toJson() =
    NextStepJson(
        number = number,
        stake = stake.toEuros(),
        isRelance = isRelance,
        requiredOdds = requiredOdds,
        openBet = openBet?.toJson(),
        capitalIfWon = capitalIfWon?.toEuros(),
        securedIfWon = securedIfWon?.toEuros(),
        ifLost = ifLost.toJson(),
    )

@Serializable
data class MontanteJson(
    val id: Long,
    val name: String,
    val bankrollId: Long,
    val bankrollName: String,
    val mode: MontanteModeJson,
    val targetMultiplier: Double?,
    val stepCount: Int?,
    val targetOdds: Double,
    val excludeStake: Boolean,
    val securePct: Int,
    val relancesAllowed: Int,
    val relancesUsed: Int,
    val startCapital: Double,
    val status: MontanteStatusJson,
    val capital: Double,
    val engaged: Double,
    val secured: Double,
    val result: Double,
    val target: Double?,
    val plannedSteps: Int?,
    val currentPalier: Int,
    val totalPaliers: Int?,
    val progress: Double?,
    val successProbability: Double?,
    val createdAt: JsonInstant,
    val closedAt: JsonInstant?,
    val paliers: List<PalierJson>,
    val nextStep: NextStepJson?,
)

fun MontanteView.toJson() =
    MontanteJson(
        id = id.value,
        name = name,
        bankrollId = bankrollId.value,
        bankrollName = bankrollName,
        mode = mode.toJson(),
        targetMultiplier = targetMultiplier,
        stepCount = stepCount,
        targetOdds = targetOdds,
        excludeStake = excludeStake,
        securePct = securePct,
        relancesAllowed = relancesAllowed,
        relancesUsed = relancesUsed,
        startCapital = startCapital.toEuros(),
        status = status.toJson(),
        capital = capital.toEuros(),
        engaged = engaged.toEuros(),
        secured = secured.toEuros(),
        result = result.toEuros(),
        target = target?.toEuros(),
        plannedSteps = plannedSteps,
        currentPalier = currentPalier,
        totalPaliers = totalPaliers,
        progress = progress,
        successProbability = successProbability,
        createdAt = createdAt,
        closedAt = closedAt,
        paliers = paliers.map { it.toJson() },
        nextStep = nextStep?.toJson(),
    )

@Serializable
data class PlannedStepJson(
    val number: Int,
    val stake: Double,
    val secured: Double,
    val capitalAfter: Double,
)

fun PlannedStepView.toJson() =
    PlannedStepJson(
        number = number,
        stake = stake.toEuros(),
        secured = secured.toEuros(),
        capitalAfter = capitalAfter.toEuros(),
    )

@Serializable
data class MontantePlanJson(
    val target: Double?,
    val plannedSteps: Int?,
    val steps: List<PlannedStepJson>,
    val successProbability: Double,
    val bankrollBalance: Double,
    val bankrollBalanceAfterLaunch: Double,
)

fun MontantePlanView.toJson() =
    MontantePlanJson(
        target = target?.toEuros(),
        plannedSteps = plannedSteps,
        steps = steps.map { it.toJson() },
        successProbability = successProbability,
        bankrollBalance = bankrollBalance.toEuros(),
        bankrollBalanceAfterLaunch = bankrollBalanceAfterLaunch.toEuros(),
    )
