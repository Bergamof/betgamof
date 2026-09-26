package fr.bergamof.betgamof.domain

import java.time.Instant

private const val MAX_SECURE_PCT = 90
private const val MAX_RELANCES = 10

enum class MontanteMode { OBJECTIVE, STEPS, FREE }

enum class MontanteStatus { ACTIVE, SUCCEEDED, BROKEN, CLOSED }

@ConsistentCopyVisibility
data class MontanteConfig private constructor(
    val name: String,
    val bankrollId: BankrollId,
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
        ensureValid(name.isNotBlank()) { "Le nom de la montante est obligatoire" }
        ensureValid(startCapital.isPositive) { "Le capital de départ doit être positif" }
        ensureValid(targetOdds > 1.0) { "La cote visée doit être supérieure à 1" }
        ensureValid(securePct in 0..MAX_SECURE_PCT) { "La part sécurisée doit être comprise entre 0 et $MAX_SECURE_PCT %" }
        ensureValid(relancesAllowed in 0..MAX_RELANCES) { "Le nombre de relances doit être compris entre 0 et $MAX_RELANCES" }
        when (mode) {
            MontanteMode.OBJECTIVE -> ensureValid((targetMultiplier ?: 0.0) > 1.0) { "L'objectif doit être supérieur au capital de départ" }
            MontanteMode.STEPS -> ensureValid((stepCount ?: 0) >= 1) { "Le nombre de paliers doit être au moins 1" }
            MontanteMode.FREE -> Unit
        }
    }

    val secureRatio get() = securePct / 100.0

    /** A lost palier restarts the montante from its starting capital while relances remain. */
    fun allowsRelance(relancesUsed: Int) = relancesUsed < relancesAllowed

    companion object {
        @Suppress("LongParameterList") // Mirrors the data class constructor.
        fun of(
            name: String,
            bankrollId: BankrollId,
            startCapital: Money,
            targetOdds: Double,
            mode: MontanteMode,
            targetMultiplier: Double?,
            stepCount: Int?,
            excludeStake: Boolean,
            securePct: Int,
            relancesAllowed: Int,
        ) = MontanteConfig(
            name.trim(),
            bankrollId,
            startCapital,
            targetOdds,
            mode,
            targetMultiplier,
            stepCount,
            excludeStake,
            securePct,
            relancesAllowed,
        )
    }
}

data class Montante(
    val id: MontanteId,
    val config: MontanteConfig,
    val createdAt: Instant,
    val closedAt: Instant?,
) {
    companion object {
        /** An excluded stake leaves the bankroll for the montante's lifetime, so the bankroll must cover it. */
        fun start(
            config: MontanteConfig,
            bankroll: BankrollPosition,
            at: Instant,
        ): Montante {
            ensureTransition(!config.excludeStake || bankroll.balance >= config.startCapital) {
                "La bankroll ne couvre pas le capital de départ de la montante"
            }
            return Montante(MontanteId.NEW, config, at, null)
        }
    }
}

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

/** What losing the current palier would leave. */
data class LossOutcome(
    /** True when a relance is still available: the montante restarts from its starting capital. */
    val relance: Boolean,
    val capitalAfter: Money,
    val securedKept: Money,
    val lostAmount: Money,
)

/** A montante together with the state replayed from its bets: the aggregate the use cases work on. */
data class TrackedMontante(
    val montante: Montante,
    val state: MontanteState,
) {
    val id get() = montante.id

    val config get() = montante.config

    /** Share of the way from the starting capital to the target; null in free mode. */
    val progress: Double?
        get() {
            val start = config.startCapital
            return state.target?.let { target -> ((state.capital - start) / (target - start)).coerceIn(0.0, 1.0) }
        }

    /** Paliers played plus those still planned; null for an active free montante. */
    val totalPaliers: Int?
        get() =
            if (state.isActive) {
                state.remainingSteps?.let { state.paliers.size + it.coerceAtLeast(1) }
            } else {
                state.paliers.size
            }

    /** Manual closing date, or the settlement of the last palier for a montante that ended by itself. */
    val endedAt: Instant?
        get() =
            montante.closedAt ?: state.paliers
                .lastOrNull()
                ?.bet
                ?.settledAt
                ?.takeIf { !state.isActive }

    val canRelance get() = config.allowsRelance(state.relancesUsed)

    /** Chance of winning every remaining palier at the target odds; null once there is nothing left to win. */
    val remainingSuccessProbability: Double?
        get() =
            state.remainingSteps
                ?.takeIf { state.isActive && it > 0 }
                ?.let { MontantePlanner.successProbability(config.targetOdds, it) }

    /** Minimum odds for the next palier to stay on track for the target. */
    val requiredOdds: Double?
        get() =
            state.target?.let { target ->
                MontantePlanner.requiredOdds(state.capital, target, (state.remainingSteps ?: 1).coerceAtLeast(1), config.secureRatio)
            }

    fun gainIfWon(odds: Double) = MontantePlanner.winPalier(state.capital, odds, config.secureRatio)

    fun lossScenario() =
        LossOutcome(
            relance = canRelance,
            capitalAfter = if (canRelance) config.startCapital else Money.ZERO,
            securedKept = state.secured,
            lostAmount = state.capital,
        )

    /** A palier always stakes the whole current capital, on the montante's bankroll. */
    fun placePalier(
        bet: NewBet,
        placedAt: Instant,
    ): Bet {
        ensureTransition(state.isActive) { "La montante « ${config.name} » est terminée" }
        ensureTransition(state.openBet == null) { "Le palier ${state.currentPalierNumber} a déjà un pari en cours" }
        ensureValid(bet.bankrollId == config.bankrollId) { "Un palier se joue sur la bankroll de la montante" }
        val palier = NewBet.of(bet.bankrollId, bet.type, bet.bookmaker, state.capital, bet.odds, bet.startsAt, bet.selections)
        return Bet.place(palier, placedAt, id)
    }

    fun close(at: Instant): Montante {
        ensureTransition(state.isActive) { "La montante « ${config.name} » est déjà terminée" }
        ensureTransition(state.openBet == null) { "Règle d'abord le pari du palier ${state.currentPalierNumber}" }
        return montante.copy(closedAt = at)
    }

    companion object {
        fun of(
            montante: Montante,
            bets: List<Bet>,
        ) = TrackedMontante(montante, MontanteEngine.replay(montante, bets.filter { it.montanteId == montante.id }))
    }
}
