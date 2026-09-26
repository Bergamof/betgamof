package fr.bergamof.betgamof.application.service

import fr.bergamof.betgamof.application.model.BankrollView
import fr.bergamof.betgamof.application.model.BetView
import fr.bergamof.betgamof.application.model.LossScenarioView
import fr.bergamof.betgamof.application.model.MontanteView
import fr.bergamof.betgamof.application.model.NextStepView
import fr.bergamof.betgamof.application.model.PalierView
import fr.bergamof.betgamof.application.model.SegmentView
import fr.bergamof.betgamof.application.model.SelectionView
import fr.bergamof.betgamof.domain.Bankroll
import fr.bergamof.betgamof.domain.Bet
import fr.bergamof.betgamof.domain.Palier
import fr.bergamof.betgamof.domain.Portfolio
import fr.bergamof.betgamof.domain.Segment
import fr.bergamof.betgamof.domain.Selection
import fr.bergamof.betgamof.domain.TrackedMontante

// Copies domain objects into read models. Every figure is computed by the domain; nothing is decided here.

internal fun Bankroll.toView(portfolio: Portfolio): BankrollView {
    val position = portfolio.position(id)
    val own = portfolio.bets.filter { it.bankrollId == id }
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

private fun Selection.toView() = SelectionView(eventId, eventName, sport, competition, market, pick, odds)

internal fun Bet.toView(portfolio: Portfolio): BetView {
    val montante = montanteId?.let(portfolio::montante)
    val palierNumber = montante?.state?.let { state -> state.paliers.firstOrNull { it.bet.id == id }?.number ?: state.currentPalierNumber }
    return BetView(
        id = id,
        bankrollId = bankrollId,
        bankrollName = portfolio.requireBankroll(bankrollId).settings.name,
        montanteId = montanteId,
        montanteName = montante?.config?.name,
        palierNumber = palierNumber,
        type = type,
        bookmaker = bookmaker,
        stake = stake,
        odds = odds,
        status = status,
        cashout = cashout,
        profit = profit,
        potentialReturn = potentialReturn,
        sport = sport,
        selections = selections.map { it.toView() },
        placedAt = placedAt,
        startsAt = startsAt,
        settledAt = settledAt,
    )
}

internal fun Segment.toView() = SegmentView(key, count, staked, profit, yield)

internal fun TrackedMontante.toView(portfolio: Portfolio): MontanteView =
    MontanteView(
        id = id,
        name = config.name,
        bankrollId = config.bankrollId,
        bankrollName = portfolio.requireBankroll(config.bankrollId).settings.name,
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
        totalPaliers = totalPaliers,
        progress = progress,
        successProbability = remainingSuccessProbability,
        createdAt = montante.createdAt,
        closedAt = endedAt,
        paliers = state.paliers.map { it.toView() },
        nextStep = if (state.isActive) nextStep(portfolio) else null,
    )

private fun Palier.toView() =
    PalierView(
        number = number,
        betId = bet.id,
        type = bet.type,
        selections = bet.selections.map { it.toView() },
        bookmaker = bet.bookmaker,
        startsAt = bet.startsAt,
        stake = capitalBefore,
        odds = bet.odds,
        status = bet.status,
        secured = secured,
        capitalAfter = capitalAfter,
        isRelance = isRelance,
    )

private fun TrackedMontante.nextStep(portfolio: Portfolio): NextStepView {
    val openBet = state.openBet
    val gainIfWon = openBet?.let { gainIfWon(it.odds) }
    val loss = lossScenario()
    return NextStepView(
        number = state.currentPalierNumber,
        stake = state.capital,
        isRelance = state.nextIsRelance,
        requiredOdds = requiredOdds,
        openBet = openBet?.toView(portfolio),
        capitalIfWon = gainIfWon?.capitalAfter,
        securedIfWon = gainIfWon?.secured,
        ifLost = LossScenarioView(loss.relance, loss.capitalAfter, loss.securedKept, loss.lostAmount),
    )
}
