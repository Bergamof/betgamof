package fr.bergamof.betgamof.business.service

import fr.bergamof.betgamof.business.NotFoundException
import fr.bergamof.betgamof.business.domain.BetStatus
import fr.bergamof.betgamof.business.domain.NewRule
import fr.bergamof.betgamof.business.domain.RuleKind
import fr.bergamof.betgamof.business.port.inbound.StatsPeriod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InsightServiceTest {
    private val fixture = ServiceFixture()
    private val insights = fixture.insights
    private val bankrollId = fixture.bankroll()

    @Test
    fun `stats summarize the settled bets`() {
        fixture.placeAndSettle(fixture.newBet(bankrollId, stake = 10, odds = 2.0), BetStatus.WON)
        fixture.placeAndSettle(fixture.newBet(bankrollId, stake = 10, odds = 2.0), BetStatus.LOST)
        fixture.bets.create(fixture.newBet(bankrollId))

        val stats = insights.stats(StatsPeriod.ALL, null)

        assertEquals(2, stats.settledCount)
        assertEquals(1, stats.openCount)
        assertEquals(0.5, stats.hitRate)
        assertEquals(2.0, stats.breakEvenOdds)
        assertEquals(10.0 / 1000, stats.averageStakeShare)
    }

    @Test
    fun `stats can be restricted to one bankroll`() {
        val other = fixture.bankroll()
        fixture.placeAndSettle(fixture.newBet(other), BetStatus.WON)
        fixture.montantes.create(fixture.montanteConfig(other))

        val stats = insights.stats(StatsPeriod.ALL, bankrollId)

        assertEquals(0, stats.settledCount)
        assertEquals(0, stats.montantes.launched)
        assertEquals(1, insights.stats(StatsPeriod.DAYS_30, other).montantes.launched)
    }

    @Test
    fun `stats on an unknown bankroll are not found`() {
        assertFailsWith<NotFoundException> { insights.stats(StatsPeriod.ALL, 42) }
    }

    @Test
    fun `stats of an empty bankroll have no stake share`() {
        val empty = fixture.bankroll(initialBalance = 0)

        assertEquals(0.0, insights.stats(StatsPeriod.MONTHS_3, empty).averageStakeShare)
    }

    @Test
    fun `rules report whether they are respected`() {
        fixture.bets.create(fixture.newBet(bankrollId, stake = 50))
        insights.addRule(NewRule(RuleKind.MAX_STAKE_PCT, 3))

        val rules = insights.addRule(NewRule(RuleKind.SINGLE_ACTIVE_MONTANTE, 0))

        assertEquals(listOf(false, true), rules.map { it.respected })
    }

    @Test
    fun `a rule can be deleted`() {
        val rule = insights.addRule(NewRule(RuleKind.PAUSE_AFTER_LOSSES, 2)).single()

        insights.deleteRule(rule.id)

        assertEquals(emptyList(), insights.rules())
    }

    @Test
    fun `deleting an unknown rule is not found`() {
        assertFailsWith<NotFoundException> { insights.deleteRule(42) }
    }
}
