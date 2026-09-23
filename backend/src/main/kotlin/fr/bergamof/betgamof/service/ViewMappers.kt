package fr.bergamof.betgamof.service

import fr.bergamof.betgamof.domain.Bankroll
import fr.bergamof.betgamof.domain.BankrollPosition
import fr.bergamof.betgamof.domain.Bet
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.Montante
import fr.bergamof.betgamof.domain.MontantePlanner
import fr.bergamof.betgamof.domain.MontanteState
import fr.bergamof.betgamof.domain.Palier
import fr.bergamof.betgamof.domain.Segment

fun Bet.label(): String =
    when (type) {
        BetType.SIMPLE -> selections.first().let { "${it.eventName} · ${it.pick}" }
        BetType.COMBINE -> "Combiné ${selections.size} sélections"
        BetType.SYSTEME -> "Système ${selections.size} sélections"
    }

fun Bet.competition(): String = selections.map { it.competition }.distinct().joinToString(" · ")

fun Bankroll.toView(
    position: BankrollPosition,
    bets: List<Bet>,
): BankrollView {
    val own = bets.filter { it.bankrollId == id }
    return BankrollView(
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
        betCount = own.size,
        bookmakers =
            own
                .groupingBy { it.bookmaker }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key },
    )
}

fun Bet.toView(snapshot: PortfolioSnapshot): BetView {
    val montanteEntry = montanteId?.let { id -> snapshot.montantes.firstOrNull { (montante, _) -> montante.id == id } }
    val palierNumber =
        montanteEntry?.let { (_, state) ->
            state.paliers.firstOrNull { it.bet.id == id }?.number ?: state.currentPalierNumber
        }
    return BetView(
        id = id,
        bankrollId = bankrollId,
        bankrollName = snapshot.bankroll(bankrollId).settings.name,
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

fun Segment.toView() = SegmentView(label, count, staked, profit, yield)

fun montanteView(
    montante: Montante,
    state: MontanteState,
    snapshot: PortfolioSnapshot,
): MontanteView {
    val config = montante.config
    val remainingSteps = state.remainingSteps
    return MontanteView(
        id = montante.id,
        name = config.name,
        bankrollId = config.bankrollId,
        bankrollName = snapshot.bankroll(config.bankrollId).settings.name,
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
        nextStep = if (state.isActive) nextStep(montante, state, snapshot, remainingSteps) else null,
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
    snapshot: PortfolioSnapshot,
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
        openBet = openBet?.toView(snapshot),
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
