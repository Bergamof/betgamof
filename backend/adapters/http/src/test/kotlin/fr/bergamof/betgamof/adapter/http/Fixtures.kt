package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.model.BankrollView
import fr.bergamof.betgamof.application.model.BetView
import fr.bergamof.betgamof.application.model.KellyView
import fr.bergamof.betgamof.application.model.LossScenarioView
import fr.bergamof.betgamof.application.model.MarketView
import fr.bergamof.betgamof.application.model.MontantePlanView
import fr.bergamof.betgamof.application.model.MontanteSummaryView
import fr.bergamof.betgamof.application.model.MontanteView
import fr.bergamof.betgamof.application.model.NextStepView
import fr.bergamof.betgamof.application.model.OutcomeView
import fr.bergamof.betgamof.application.model.PalierView
import fr.bergamof.betgamof.application.model.PlannedStepView
import fr.bergamof.betgamof.application.model.ProfitPointView
import fr.bergamof.betgamof.application.model.RuleView
import fr.bergamof.betgamof.application.model.SegmentView
import fr.bergamof.betgamof.application.model.SelectionView
import fr.bergamof.betgamof.application.model.SportEventView
import fr.bergamof.betgamof.application.model.StatsView
import fr.bergamof.betgamof.application.model.StreakView
import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.BetId
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.MarketCategory
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteId
import fr.bergamof.betgamof.domain.MontanteMode
import fr.bergamof.betgamof.domain.MontanteStatus
import fr.bergamof.betgamof.domain.OddsRange
import fr.bergamof.betgamof.domain.RuleId
import fr.bergamof.betgamof.domain.RuleKind
import fr.bergamof.betgamof.domain.SegmentKey
import java.time.Instant

// Canned read models returned by the fakes.

val placedAt: Instant = Instant.parse("2026-09-10T20:00:00Z")
val startsAt: Instant = Instant.parse("2026-09-11T19:00:00Z")

val sampleBankroll =
    BankrollView(
        id = BankrollId(7),
        name = "Principale",
        color = BankrollColor.CITRON,
        initialBalance = Money.euros(1000),
        balance = Money.euros(1018.5),
        stopLoss = Money.euros(900),
        stopLossMargin = Money.euros(118.5),
        kellyFraction = 0.25,
        fixedStake = null,
        outsideMontantes = Money.euros(1018.5),
        openStake = Money.ZERO,
        staked = Money.euros(25),
        profit = Money.euros(18.5),
        roi = 0.74,
        betCount = 1,
        bookmakers = listOf("Winamax"),
    )

val lensLyon = SelectionView("evt-1", "Lens – Lyon", "Football", "Ligue 1", "Buts", "Plus de 1,5", 1.74)
val psgOm = SelectionView(null, "PSG – OM", "Football", "Ligue 1", "Résultat", "PSG", 1.5)
val nadal = SelectionView(null, "Nadal – Federer", "Tennis", "Roland-Garros", "Vainqueur", "Nadal", 2.0)

val simpleBet =
    BetView(
        id = BetId(42),
        bankrollId = BankrollId(7),
        bankrollName = "Principale",
        montanteId = null,
        montanteName = null,
        palierNumber = null,
        type = BetType.SIMPLE,
        bookmaker = "Winamax",
        stake = Money.euros(25),
        odds = 1.74,
        status = BetStatus.WON,
        cashout = null,
        profit = Money.euros(18.5),
        potentialReturn = Money.euros(43.5),
        sport = "Football",
        selections = listOf(lensLyon),
        placedAt = placedAt,
        startsAt = startsAt,
        settledAt = Instant.parse("2026-09-11T21:00:00Z"),
    )

val combinedBet =
    simpleBet.copy(
        id = BetId(43),
        montanteId = MontanteId(3),
        montanteName = "Montante été",
        palierNumber = 2,
        type = BetType.COMBINE,
        status = BetStatus.CASHOUT,
        cashout = Money.euros(30),
        sport = "Multi",
        selections = listOf(lensLyon, psgOm, nadal),
        settledAt = null,
    )

val samplePalier =
    PalierView(
        number = 1,
        betId = BetId(44),
        type = BetType.SYSTEME,
        selections = listOf(lensLyon, psgOm),
        bookmaker = "Betclic",
        startsAt = startsAt,
        stake = Money.euros(100),
        odds = 2.5,
        status = BetStatus.LOST,
        secured = Money.euros(10),
        capitalAfter = Money.ZERO,
        isRelance = false,
    )

val sampleMontante =
    MontanteView(
        id = MontanteId(3),
        name = "Montante été",
        bankrollId = BankrollId(7),
        bankrollName = "Principale",
        mode = MontanteMode.OBJECTIVE,
        targetMultiplier = 3.0,
        stepCount = null,
        targetOdds = 1.5,
        excludeStake = true,
        securePct = 10,
        relancesAllowed = 1,
        relancesUsed = 1,
        startCapital = Money.euros(100),
        status = MontanteStatus.BROKEN,
        capital = Money.ZERO,
        engaged = Money.euros(200),
        secured = Money.euros(10),
        result = Money.euros(-190),
        target = Money.euros(300),
        plannedSteps = 3,
        currentPalier = 2,
        totalPaliers = 3,
        progress = 0.5,
        successProbability = null,
        createdAt = placedAt,
        closedAt = null,
        paliers = listOf(samplePalier),
        nextStep =
            NextStepView(
                number = 2,
                stake = Money.euros(100),
                isRelance = true,
                requiredOdds = 1.5,
                openBet = combinedBet,
                capitalIfWon = Money.euros(150),
                securedIfWon = null,
                ifLost = LossScenarioView(false, Money.ZERO, Money.euros(10), Money.euros(90)),
            ),
    )

val samplePlan =
    MontantePlanView(
        target = Money.euros(480),
        plannedSteps = 1,
        steps = listOf(PlannedStepView(1, Money.euros(160), Money.euros(24), Money.euros(216))),
        successProbability = 0.3,
        bankrollBalance = Money.euros(1000),
        bankrollBalanceAfterLaunch = Money.euros(840),
    )

val sampleKelly = KellyView(Money.euros(12), 0.012, 0.6)

val sampleRule = RuleView(RuleId(5), RuleKind.PAUSE_AFTER_LOSSES, 3, respected = true)

fun segment(key: SegmentKey) = SegmentView(key, 2, Money.euros(20), Money.euros(4), 0.2)

val sampleStats =
    StatsView(
        settledCount = 2,
        openCount = 1,
        staked = Money.euros(20),
        profit = Money.euros(4),
        yield = 0.2,
        won = 1,
        lost = 1,
        hitRate = 0.5,
        averageOdds = 2.2,
        breakEvenOdds = 2.0,
        averageStake = Money.euros(10),
        averageStakeShare = 0.01,
        bestWinStreak = 1,
        currentStreak = StreakView(won = false, length = 1),
        maxDrawdown = Money.euros(10),
        firstBetAt = placedAt,
        profitCurve = listOf(ProfitPointView(startsAt, Money.euros(4))),
        bySport = listOf(segment(SegmentKey.Sport("Tennis"))),
        byOddsRange = OddsRange.entries.map { segment(SegmentKey.Odds(it)) },
        byMarket = listOf(segment(SegmentKey.Market("Buts")), segment(SegmentKey.MultipleMarkets)),
        lateNight = segment(SegmentKey.LateNight),
        placedDayBefore = segment(SegmentKey.DayBefore),
        montantes = MontanteSummaryView(1, 0, 1, 0, 0, 1.0, null),
    )

val sampleEvent =
    SportEventView(
        id = "evt-1",
        sport = "Football",
        competition = "Ligue 1",
        name = "Lens – Lyon",
        startsAt = startsAt,
        markets =
            listOf(
                MarketView("m-1", "Mi-temps", MarketCategory.MI_TEMPS, false, listOf(OutcomeView("Lens", 2.4, "Unibet"))),
            ),
    )
