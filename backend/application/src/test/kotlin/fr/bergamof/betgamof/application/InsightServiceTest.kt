package fr.bergamof.betgamof.application

import fr.bergamof.betgamof.application.fake.InMemoryPersistence
import fr.bergamof.betgamof.application.port.inbound.AddRuleCommand
import fr.bergamof.betgamof.application.port.inbound.BankrollCommand
import fr.bergamof.betgamof.application.port.inbound.PlaceBetCommand
import fr.bergamof.betgamof.application.port.inbound.SelectionCommand
import fr.bergamof.betgamof.application.port.inbound.SettleBetCommand
import fr.bergamof.betgamof.application.port.inbound.StartMontanteCommand
import fr.bergamof.betgamof.application.port.inbound.StatsPeriod
import fr.bergamof.betgamof.application.service.BankrollService
import fr.bergamof.betgamof.application.service.BetService
import fr.bergamof.betgamof.application.service.InsightService
import fr.bergamof.betgamof.application.service.MontanteService
import fr.bergamof.betgamof.application.service.PortfolioLoader
import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.InvalidValueException
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteMode
import fr.bergamof.betgamof.domain.OddsRange
import fr.bergamof.betgamof.domain.RuleId
import fr.bergamof.betgamof.domain.RuleKind
import fr.bergamof.betgamof.domain.SegmentKey
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InsightServiceTest {
    private val now = Instant.parse("2026-09-11T16:00:00Z")
    private val clock = MovableClock(now)
    private val store = InMemoryPersistence()
    private val portfolio = PortfolioLoader(store.bankrolls, store.bets, store.montantes)
    private val bankrolls = BankrollService(portfolio, store.bankrolls, store.transactions, clock)
    private val bets = BetService(portfolio, store.bets, store.transactions, clock)
    private val montantes = MontanteService(portfolio, store.montantes, store.transactions, clock)
    private val insights = InsightService(portfolio, store.rules, store.transactions, clock, ZoneOffset.UTC)

    private val main = bankrolls.create(BankrollCommand("Principale", BankrollColor.GAZON, Money.euros(1000))).id

    /** Places a bet at [placedAt] (now by default) starting [startsIn] later, and settles it when [result] is given. */
    private fun bet(
        stake: Int,
        odds: Double,
        result: BetStatus? = null,
        placedAt: Instant = now,
        bankroll: BankrollId = main,
        sport: String = "Football",
        market: String = "Total de buts",
        startsIn: Duration = HOURS_3,
    ) {
        clock.now = placedAt
        val selection = SelectionCommand(null, "Lens – Lyon", sport, "Ligue 1", market, "Plus de 1,5", odds)
        val placed =
            bets.place(
                PlaceBetCommand(
                    bankroll,
                    null,
                    BetType.SIMPLE,
                    "Winamax",
                    Money.euros(stake),
                    odds,
                    placedAt + startsIn,
                    listOf(selection),
                ),
            )
        result?.let { bets.settle(placed.id, SettleBetCommand(it)) }
        clock.now = now
    }

    private fun startMontante(name: String) =
        montantes.start(StartMontanteCommand(name, main, Money.euros(50), 1.6, MontanteMode.STEPS, null, 3, false, 20, 0))

    @Test
    fun `stats only count the bets placed during the period`() {
        bet(20, 2.0, BetStatus.WON, placedAt = now - Duration.ofDays(10))
        bet(10, 2.0, BetStatus.LOST, placedAt = now - Duration.ofDays(60))
        bet(30, 2.0, BetStatus.LOST, placedAt = now - Duration.ofDays(200))

        assertEquals(1, insights.stats(StatsPeriod.DAYS_30, null).settledCount)
        assertEquals(2, insights.stats(StatsPeriod.MONTHS_3, null).settledCount)
        val all = insights.stats(StatsPeriod.ALL, null)
        assertEquals(3, all.settledCount)
        assertEquals(Money.euros(-20), all.profit)
    }

    @Test
    fun `stats can be restricted to one bankroll`() {
        val other = bankrolls.create(BankrollCommand("Fun", BankrollColor.CIEL, Money.euros(100))).id
        bet(20, 2.0, BetStatus.WON)
        bet(5, 4.0, BetStatus.LOST, bankroll = other)
        bet(5, 4.0, bankroll = other)

        val stats = insights.stats(StatsPeriod.ALL, other)

        assertEquals(1, stats.settledCount)
        assertEquals(1, stats.openCount)
        assertEquals(Money.euros(-5), stats.profit)
        assertFailsWith<NotFoundException> { insights.stats(StatsPeriod.ALL, BankrollId(42)) }
    }

    @Test
    fun `the average stake is compared with the current balance`() {
        bet(20, 2.0, BetStatus.WON)
        bet(40, 2.0, BetStatus.LOST)

        val stats = insights.stats(StatsPeriod.ALL, main)

        // Balance 1000 + 20 - 40 = 980; average stake 30.
        assertEquals(Money.euros(30), stats.averageStake)
        assertEquals(30.0 / 980, stats.averageStakeShare, 1e-9)
    }

    @Test
    fun `stats are segmented by sport, market, odds range and habits`() {
        bet(20, 1.8, BetStatus.WON)
        bet(10, 2.5, BetStatus.LOST, sport = "Tennis", market = "Vainqueur")
        bet(10, 1.4, BetStatus.WON, placedAt = Instant.parse("2026-09-10T22:30:00Z"))
        bet(10, 1.4, BetStatus.LOST, placedAt = Instant.parse("2026-09-09T22:00:00Z"), startsIn = Duration.ofHours(1))

        val stats = insights.stats(StatsPeriod.ALL, null)

        assertEquals(setOf(SegmentKey.Sport("Football"), SegmentKey.Sport("Tennis")), stats.bySport.map { it.key }.toSet())
        assertEquals(setOf(SegmentKey.Market("Total de buts"), SegmentKey.Market("Vainqueur")), stats.byMarket.map { it.key }.toSet())
        assertEquals(OddsRange.entries.map { SegmentKey.Odds(it) }, stats.byOddsRange.map { it.key })
        assertEquals(listOf(2, 1, 1, 0), stats.byOddsRange.map { it.count })
        assertEquals(SegmentKey.LateNight, stats.lateNight.key)
        assertEquals(2, stats.lateNight.count)
        assertEquals(SegmentKey.DayBefore, stats.placedDayBefore.key)
        assertEquals(1, stats.placedDayBefore.count)
    }

    @Test
    fun `a combined bet is segmented as multiple markets`() {
        val selections =
            listOf(
                SelectionCommand(null, "ASVEL – Monaco", "Basket", "Euroligue", "Vainqueur", "Monaco", 1.55),
                SelectionCommand(null, "Real – Fenerbahçe", "Basket", "Euroligue", "Vainqueur", "Real", 1.50),
            )
        val combo = bets.place(PlaceBetCommand(main, null, BetType.COMBINE, "Unibet", Money.euros(10), 2.32, now + HOURS_3, selections))
        bets.settle(combo.id, SettleBetCommand(BetStatus.WON))

        assertEquals(listOf(SegmentKey.MultipleMarkets), insights.stats(StatsPeriod.ALL, null).byMarket.map { it.key })
    }

    @Test
    fun `rules are respected when the history follows them`() {
        insights.addRule(AddRuleCommand(RuleKind.MAX_STAKE_PCT, 3))
        insights.addRule(AddRuleCommand(RuleKind.PAUSE_AFTER_LOSSES, 2))
        insights.addRule(AddRuleCommand(RuleKind.SINGLE_ACTIVE_MONTANTE, 0))
        bet(20, 2.0, BetStatus.LOST, placedAt = now - Duration.ofDays(2))
        bet(20, 2.0, BetStatus.WON, placedAt = now - Duration.ofDays(1))
        startMontante("Montante Tennis")

        assertEquals(listOf(true, true, true), insights.rules().map { it.respected })
    }

    @Test
    fun `rules are broken when the history ignores them`() {
        val stake = insights.addRule(AddRuleCommand(RuleKind.MAX_STAKE_PCT, 3)).single().id
        bet(50, 2.0)
        assertEquals(false, insights.rules().single { it.id == stake }.respected)

        insights.deleteRule(stake)
        insights.addRule(AddRuleCommand(RuleKind.PAUSE_AFTER_LOSSES, 2))
        insights.addRule(AddRuleCommand(RuleKind.SINGLE_ACTIVE_MONTANTE, 0))
        bet(10, 2.0, BetStatus.LOST, placedAt = now - Duration.ofDays(3))
        bet(10, 2.0, BetStatus.LOST, placedAt = now - Duration.ofDays(2))
        bet(10, 2.0, placedAt = now - Duration.ofDays(1))
        startMontante("Montante Tennis")
        startMontante("Montante Liga")

        assertEquals(
            mapOf(RuleKind.PAUSE_AFTER_LOSSES to false, RuleKind.SINGLE_ACTIVE_MONTANTE to false),
            insights.rules().associate { it.kind to it.respected },
        )
    }

    @Test
    fun `an invalid rule is rejected by the domain`() {
        assertFailsWith<InvalidValueException> { insights.addRule(AddRuleCommand(RuleKind.MAX_STAKE_PCT, 0)) }
        assertFailsWith<InvalidValueException> { insights.addRule(AddRuleCommand(RuleKind.PAUSE_AFTER_LOSSES, -1)) }
        assertTrue(insights.rules().isEmpty())
    }

    @Test
    fun `deleting a rule removes it and an unknown rule is not found`() {
        val id = insights.addRule(AddRuleCommand(RuleKind.PAUSE_AFTER_LOSSES, 2)).single().id

        insights.deleteRule(id)

        assertTrue(insights.rules().isEmpty())
        assertFailsWith<NotFoundException> { insights.deleteRule(id) }
        assertFailsWith<NotFoundException> { insights.deleteRule(RuleId(42)) }
    }

    /** A clock the test sets by hand, to date bets in the past. */
    private class MovableClock(
        var now: Instant,
    ) : Clock() {
        override fun instant(): Instant = now

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = fixed(now, zone)
    }

    private companion object {
        val HOURS_3: Duration = Duration.ofHours(3)
    }
}
