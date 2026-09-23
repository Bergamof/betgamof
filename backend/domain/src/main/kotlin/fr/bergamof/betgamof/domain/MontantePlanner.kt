package fr.bergamof.betgamof.domain

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

private const val FREE_MODE_PREVIEW_STEPS = 5
private const val MAX_PLANNED_STEPS = 30

data class PlannedStep(
    val number: Int,
    val stake: Money,
    val secured: Money,
    val capitalAfter: Money,
)

data class MontantePlan(
    val target: Money?,
    val plannedSteps: Int?,
    val steps: List<PlannedStep>,
    val successProbability: Double,
)

/** Result of winning one palier: part of the gain is set aside, the rest is re-staked. */
data class PalierGain(
    val secured: Money,
    val capitalAfter: Money,
)

/** Pure calculations shared by the creation preview and the live montante engine. */
object MontantePlanner {
    fun winPalier(
        capital: Money,
        odds: Double,
        secureRatio: Double,
    ): PalierGain {
        val gain = capital * (odds - 1)
        val secured = if (gain.isPositive) gain * secureRatio else Money.ZERO
        return PalierGain(secured, capital + gain - secured)
    }

    fun growthFactor(
        odds: Double,
        secureRatio: Double,
    ) = 1 + (odds - 1) * (1 - secureRatio)

    fun plan(config: MontanteConfig): MontantePlan {
        val plannedSteps = plannedSteps(config)
        val steps = mutableListOf<PlannedStep>()
        var capital = config.startCapital
        repeat(plannedSteps ?: FREE_MODE_PREVIEW_STEPS) { index ->
            val gain = winPalier(capital, config.targetOdds, config.secureRatio)
            steps += PlannedStep(index + 1, capital, gain.secured, gain.capitalAfter)
            capital = gain.capitalAfter
        }
        val target =
            when (config.mode) {
                MontanteMode.OBJECTIVE -> config.startCapital * requireNotNull(config.targetMultiplier)
                MontanteMode.STEPS -> capital
                MontanteMode.FREE -> null
            }
        return MontantePlan(target, plannedSteps, steps, successProbability(config.targetOdds, plannedSteps))
    }

    fun plannedSteps(config: MontanteConfig): Int? =
        when (config.mode) {
            MontanteMode.OBJECTIVE -> {
                val growth = growthFactor(config.targetOdds, config.secureRatio)
                ceil(ln(requireNotNull(config.targetMultiplier)) / ln(growth)).toInt().coerceIn(1, MAX_PLANNED_STEPS)
            }
            MontanteMode.STEPS -> config.stepCount
            MontanteMode.FREE -> null
        }

    /** Chance of winning every remaining palier, using the bookmaker's implied probability. */
    fun successProbability(
        odds: Double,
        steps: Int?,
    ): Double = steps?.let { (1 / odds).pow(it) } ?: 0.0

    /** Minimum odds to reach [target] from [capital] within [remainingSteps] paliers. */
    fun requiredOdds(
        capital: Money,
        target: Money,
        remainingSteps: Int,
        secureRatio: Double,
    ): Double? {
        if (capital >= target || remainingSteps < 1 || !capital.isPositive) return null
        val growthPerStep = (target / capital).pow(1.0 / remainingSteps)
        return 1 + (growthPerStep - 1) / (1 - secureRatio)
    }
}
