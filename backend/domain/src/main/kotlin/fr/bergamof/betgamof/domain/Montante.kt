package fr.bergamof.betgamof.domain

import java.time.Instant

private const val MAX_SECURE_PCT = 90
private const val MAX_RELANCES = 10

enum class MontanteMode { OBJECTIVE, STEPS, FREE }

enum class MontanteStatus { ACTIVE, SUCCEEDED, BROKEN, CLOSED }

data class MontanteConfig(
    val name: String,
    val bankrollId: Long,
    val startCapital: Money,
    val targetOdds: Double,
    val mode: MontanteMode,
    val targetMultiplier: Double?,
    val stepCount: Int?,
    val excludeStake: Boolean,
    val securePct: Int,
    val relancesAllowed: Int,
) {
    init {
        require(name.isNotBlank()) { "Le nom de la montante est obligatoire" }
        require(startCapital.isPositive) { "Le capital de départ doit être positif" }
        require(targetOdds > 1.0) { "La cote visée doit être supérieure à 1" }
        require(securePct in 0..MAX_SECURE_PCT) { "La part sécurisée doit être comprise entre 0 et $MAX_SECURE_PCT %" }
        require(relancesAllowed in 0..MAX_RELANCES) { "Le nombre de relances doit être compris entre 0 et $MAX_RELANCES" }
        when (mode) {
            MontanteMode.OBJECTIVE -> require((targetMultiplier ?: 0.0) > 1.0) { "L'objectif doit être supérieur au capital de départ" }
            MontanteMode.STEPS -> require((stepCount ?: 0) >= 1) { "Le nombre de paliers doit être au moins 1" }
            MontanteMode.FREE -> Unit
        }
    }

    val secureRatio get() = securePct / 100.0
}

data class Montante(
    val id: Long,
    val config: MontanteConfig,
    val createdAt: Instant,
    val closedAt: Instant?,
)

/** One settled bet of a montante, with the capital it produced. */
data class Palier(
    val number: Int,
    val bet: Bet,
    val capitalBefore: Money,
    val secured: Money,
    val capitalAfter: Money,
    val isRelance: Boolean,
)

data class MontanteState(
    val status: MontanteStatus,
    val capital: Money,
    val engaged: Money,
    val secured: Money,
    val relancesUsed: Int,
    /** Paliers won since the start or the last relance. */
    val winsInRun: Int,
    val paliers: List<Palier>,
    val openBet: Bet?,
    val nextIsRelance: Boolean,
    val target: Money?,
    val plannedSteps: Int?,
) {
    val isActive get() = status == MontanteStatus.ACTIVE

    val currentPalierNumber get() = paliers.size + 1

    /** Paliers still needed to reach the plan, counted from the last relance. */
    val remainingSteps get() = plannedSteps?.let { (it - winsInRun).coerceAtLeast(0) }

    /** Net result for the bankroll once everything is paid back: capital + secured - engaged. */
    val result get() = capital + secured - engaged
}
