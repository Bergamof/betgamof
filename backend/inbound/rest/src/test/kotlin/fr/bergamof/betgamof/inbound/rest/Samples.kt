package fr.bergamof.betgamof.inbound.rest

import fr.bergamof.betgamof.business.domain.BankrollColor
import fr.bergamof.betgamof.business.domain.BetStatus
import fr.bergamof.betgamof.business.domain.BetType
import fr.bergamof.betgamof.business.domain.DefaultStake
import fr.bergamof.betgamof.business.domain.Market
import fr.bergamof.betgamof.business.domain.MarketCategory
import fr.bergamof.betgamof.business.domain.Money
import fr.bergamof.betgamof.business.domain.MontanteMode
import fr.bergamof.betgamof.business.domain.MontanteStatus
import fr.bergamof.betgamof.business.domain.Outcome
import fr.bergamof.betgamof.business.domain.RuleKind
import fr.bergamof.betgamof.business.domain.SportEvent
import fr.bergamof.betgamof.business.model.BankrollView
import fr.bergamof.betgamof.business.model.BetView
import fr.bergamof.betgamof.business.model.KellyView
import fr.bergamof.betgamof.business.model.LossScenario
import fr.bergamof.betgamof.business.model.MontantePlanView
import fr.bergamof.betgamof.business.model.MontanteSummaryView
import fr.bergamof.betgamof.business.model.MontanteView
import fr.bergamof.betgamof.business.model.NextStepView
import fr.bergamof.betgamof.business.model.PalierView
import fr.bergamof.betgamof.business.model.PlannedStepView
import fr.bergamof.betgamof.business.model.ProfitPointView
import fr.bergamof.betgamof.business.model.RuleView
import fr.bergamof.betgamof.business.model.SegmentView
import fr.bergamof.betgamof.business.model.SelectionView
import fr.bergamof.betgamof.business.model.StatsView
import fr.bergamof.betgamof.business.model.StreakView
import java.time.Instant

// Read models returned by the mocked use cases: every field set, so that every JSON mapper is exercised.

val AT: Instant = Instant.parse("2026-09-11T19:00:00Z")

fun euros(amount: Double) = Money.euros(amount)

val bankrollView =
    BankrollView(
        id = 1,
        name = "Principale",
        color = BankrollColor.GAZON,
        initialBalance = euros(1000.0),
        balance = euros(1018.5),
        stopLoss = euros(900.0),
        stopLossMargin = euros(118.5),
        kellyFraction = 0.25,
        defaultStake = DefaultStake.Percent(2.5),
        outsideMontantes = euros(160.0),
        openStake = euros(10.0),
        staked = euros(25.0),
        profit = euros(18.5),
        roi = 0.74,
        betCount = 2,
        bookmakers = listOf("Winamax"),
    )

val betView =
    BetView(
        id = 7,
        bankrollId = 1,
        bankrollName = "Principale",
        montanteId = null,
        montanteName = null,
        palierNumber = null,
        type = BetType.SIMPLE,
        bookmaker = "Winamax",
        stake = euros(25.0),
        odds = 1.72,
        status = BetStatus.WON,
        cashout = null,
        profit = euros(18.0),
        potentialReturn = euros(43.0),
        label = "Lens – Lyon · Plus de 1,5",
        sport = "Football",
        competition = "Ligue 1",
        market = "Total de buts",
        selections = listOf(SelectionView(null, "Lens – Lyon", "Football", "Ligue 1", "Total de buts", "Plus de 1,5", 1.72)),
        placedAt = AT,
        startsAt = AT,
        settledAt = AT,
    )

val montanteView =
    MontanteView(
        id = 3,
        name = "Montante Ligue 1",
        bankrollId = 1,
        bankrollName = "Principale",
        mode = MontanteMode.OBJECTIVE,
        targetMultiplier = 3.0,
        stepCount = null,
        targetOdds = 1.75,
        excludeStake = true,
        securePct = 30,
        relancesAllowed = 1,
        relancesUsed = 0,
        startCapital = euros(160.0),
        status = MontanteStatus.ACTIVE,
        capital = euros(244.0),
        engaged = euros(160.0),
        secured = euros(36.0),
        result = euros(120.0),
        target = euros(480.0),
        plannedSteps = 3,
        currentPalier = 2,
        totalPaliers = 3,
        progress = 0.26,
        successProbability = 0.33,
        createdAt = AT,
        closedAt = null,
        paliers =
            listOf(
                PalierView(1, 7, "Lens – Lyon", "Winamax", AT, euros(160.0), 1.75, BetStatus.WON, euros(36.0), euros(244.0), false),
            ),
        nextStep =
            NextStepView(
                number = 2,
                stake = euros(244.0),
                isRelance = false,
                requiredOdds = 1.9,
                openBet = betView,
                capitalIfWon = euros(372.1),
                securedIfWon = euros(55.0),
                ifLost = LossScenario(true, euros(160.0), euros(36.0), euros(244.0)),
            ),
    )

val planView =
    MontantePlanView(
        target = euros(480.0),
        plannedSteps = 1,
        steps = listOf(PlannedStepView(1, euros(160.0), euros(36.0), euros(244.0))),
        successProbability = 0.57,
        bankrollBalance = euros(1000.0),
        bankrollBalanceAfterLaunch = euros(840.0),
    )

private val segment = SegmentView("Football", 2, euros(20.0), euros(4.0), 0.2)

val statsView =
    StatsView(
        settledCount = 2,
        openCount = 1,
        staked = euros(20.0),
        profit = euros(4.0),
        yield = 0.2,
        won = 1,
        lost = 1,
        hitRate = 0.5,
        averageOdds = 2.0,
        breakEvenOdds = 2.0,
        averageStake = euros(10.0),
        averageStakeShare = 0.01,
        bestWinStreak = 1,
        currentStreak = StreakView(won = false, length = 1),
        maxDrawdown = euros(10.0),
        firstBetAt = AT,
        profitCurve = listOf(ProfitPointView(AT, euros(4.0))),
        bySport = listOf(segment),
        byOddsRange = listOf(segment),
        byMarket = listOf(segment),
        lateNight = segment,
        placedDayBefore = segment,
        montantes = MontanteSummaryView(1, 1, 0, 0, 0, 3.0, null),
    )

val ruleView = RuleView(1, RuleKind.MAX_STAKE_PCT, 3, respected = true)

val kellyView = KellyView(euros(12.0), 0.012, 0.6)

val sportEvent =
    SportEvent(
        id = "psg-om",
        sport = "Football",
        competition = "Ligue 1",
        name = "PSG – OM",
        startsAt = AT,
        markets = listOf(Market("1x2", "Résultat", MarketCategory.RESULTAT, true, listOf(Outcome("PSG", 1.6, "Winamax")))),
    )
