package fr.bergamof.betgamof.business.service

import fr.bergamof.betgamof.business.domain.Bankroll
import fr.bergamof.betgamof.business.domain.BankrollPosition
import fr.bergamof.betgamof.business.domain.Bet
import fr.bergamof.betgamof.business.domain.BetType
import fr.bergamof.betgamof.business.domain.Money
import fr.bergamof.betgamof.business.domain.Montante
import fr.bergamof.betgamof.business.domain.MontantePlanner
import fr.bergamof.betgamof.business.domain.MontanteState
import fr.bergamof.betgamof.business.domain.Palier
import fr.bergamof.betgamof.business.domain.Segment
import fr.bergamof.betgamof.business.model.BankrollView
import fr.bergamof.betgamof.business.model.BetView
import fr.bergamof.betgamof.business.model.LossScenario
import fr.bergamof.betgamof.business.model.MontanteView
import fr.bergamof.betgamof.business.model.NextStepView
import fr.bergamof.betgamof.business.model.PalierView
import fr.bergamof.betgamof.business.model.SegmentView
import fr.bergamof.betgamof.business.model.SelectionView

internal fun Bet.label(): String =
    when (type) {
        BetType.SIMPLE -> selections.first().let { "${it.eventName} · ${it.pick}" }
        BetType.COMBINE -> "Combiné ${selections.size} sélections"
        BetType.SYSTEME -> "Système ${selections.size} sélections"
    }

internal fun Bet.competition(): String = selections.map { it.competition }.distinct().joinToString(" · ")

internal fun Bankroll.toView(
    position: BankrollPosition,
    betCount: Int,
    bookmakers: List<String>,
) = BankrollView(
    id = id,
    name = settings.name,
    color = settings.color,
    initialBalance = settings.initialBalance,
    balance = position.balance,
    stopLoss = settings.stopLoss,
    stopLossMargin = settings.stopLoss?.let { position.balance - it },
    kellyFraction = settings.kellyFraction,
    fixedStake = settings.fixedStake,
    outsideMontantes = position.outsideMontantes,
    openStake = position.openStake,
    staked = position.staked,
    profit = position.profit,
    roi = position.roi,
    betCount = betCount,
    bookmakers = bookmakers,
)

/** [montanteEntry]: the montante this bet belongs to, if any, with its state. */
internal fun Bet.toView(
    bankrollName: String,
    montanteEntry: MontanteEntry?,
): BetView {
    val palierNumber =
        montanteEntry?.let { (_, state) ->
            state.paliers.firstOrNull { it.bet.id == id }?.number ?: state.currentPalierNumber
        }
    return BetView(
        id = id,
        bankrollId = bankrollId,
        bankrollName = bankrollName,
        montanteId = montanteId,
        montanteName = montanteEntry?.first?.config?.name,
        palierNumber = palierNumber,
        type = type,
        bookmaker = bookmaker,
        stake = stake,
        odds = odds,
        status = status,
        cashout = cashout,
        profit = profit,
        potentialReturn = potentialReturn,
        label = label(),
        sport = sport,
        competition = competition(),
        market = market,
        selections = selections.map { SelectionView(it.eventId, it.eventName, it.sport, it.competition, it.market, it.pick, it.odds) },
        placedAt = placedAt,
        startsAt = startsAt,
        settledAt = settledAt,
    )
}

internal fun Segment.toView() = SegmentView(label, count, staked, profit, yield)

internal fun montanteView(
    montante: Montante,
    state: MontanteState,
    bankrollName: String,
): MontanteView {
    val config = montante.config
    val remainingSteps = state.remainingSteps
    return MontanteView(
        id = montante.id,
        name = config.name,
        bankrollId = config.bankrollId,
        bankrollName = bankrollName,
        mode = config.mode,
        targetMultiplier = config.targetMultiplier,
        stepCount = config.stepCount,
        targetOdds = config.targetOdds,
        excludeStake = config.excludeStake,
        securePct = config.securePct,
        relancesAllowed = config.relancesAllowed,
        relancesUsed = state.relancesUsed,
        startCapital = config.startCapital,
        status = state.status,
        capital = state.capital,
        engaged = state.engaged,
        secured = state.secured,
        result = state.result,
        target = state.target,
        plannedSteps = state.plannedSteps,
        currentPalier = state.currentPalierNumber,
        totalPaliers = totalPaliers(state),
        progress = progress(montante, state),
        successProbability =
            remainingSteps
                ?.takeIf { state.isActive && it > 0 }
                ?.let { MontantePlanner.successProbability(config.targetOdds, it) },
        createdAt = montante.createdAt,
        closedAt =
            montante.closedAt ?: state.paliers
                .lastOrNull()
                ?.bet
                ?.settledAt
                ?.takeIf { !state.isActive },
        paliers = state.paliers.map { it.toView() },
        nextStep = if (state.isActive) nextStep(montante, state, bankrollName, remainingSteps) else null,
    )
}

private fun progress(
    montante: Montante,
    state: MontanteState,
): Double? {
    val start = montante.config.startCapital
    return state.target?.let { target -> ((state.capital - start) / (target - start)).coerceIn(0.0, 1.0) }
}

private fun totalPaliers(state: MontanteState): Int? =
    if (state.isActive) {
        state.remainingSteps?.let { state.paliers.size + it.coerceAtLeast(1) }
    } else {
        state.paliers.size
    }

private fun Palier.toView() =
    PalierView(
        number = number,
        betId = bet.id,
        label = bet.label(),
        bookmaker = bet.bookmaker,
        startsAt = bet.startsAt,
        stake = capitalBefore,
        odds = bet.odds,
        status = bet.status,
        secured = secured,
        capitalAfter = capitalAfter,
        isRelance = isRelance,
    )

private fun nextStep(
    montante: Montante,
    state: MontanteState,
    bankrollName: String,
    remainingSteps: Int?,
): NextStepView {
    val config = montante.config
    val openBet = state.openBet
    val gainIfWon = openBet?.let { MontantePlanner.winPalier(state.capital, it.odds, config.secureRatio) }
    val canRelance = state.relancesUsed < config.relancesAllowed
    return NextStepView(
        number = state.currentPalierNumber,
        stake = state.capital,
        isRelance = state.nextIsRelance,
        requiredOdds =
            state.target?.let { target ->
                MontantePlanner.requiredOdds(state.capital, target, (remainingSteps ?: 1).coerceAtLeast(1), config.secureRatio)
            },
        openBet = openBet?.toView(bankrollName, montante to state),
        capitalIfWon = gainIfWon?.capitalAfter,
        securedIfWon = gainIfWon?.secured,
        ifLost =
            LossScenario(
                relance = canRelance,
                capitalAfter = if (canRelance) config.startCapital else Money.ZERO,
                securedKept = state.secured,
                lostAmount = state.capital,
            ),
    )
}
