package fr.bergamof.betgamof.domain

import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InsightsTest {
    private val paris = ZoneId.of("Europe/Paris")

    @Test
    fun `stats compute yield, streaks and drawdown`() {
        val bets =
            listOf(
                bet(1, 2.0, Money.euros(10), BetStatus.WON),
                bet(2, 2.0, Money.euros(10), BetStatus.LOST),
                bet(3, 2.0, Money.euros(10), BetStatus.LOST),
                bet(4, 2.0, Money.euros(10), BetStatus.WON, sport = "Tennis"),
                bet(5, 2.0, Money.euros(10), BetStatus.WON, sport = "Tennis"),
                bet(6, 2.0, Money.euros(10), BetStatus.OPEN),
            )
        val stats = StatsCalculator.compute(bets, emptyList(), paris)

        assertEquals(5, stats.settledCount)
        assertEquals(Money.euros(10), stats.profit)
        assertEquals(0.2, stats.yield, 1e-9)
        assertEquals(0.6, stats.hitRate, 1e-9)
        assertEquals(2, stats.bestWinStreak)
        assertEquals(Streak(won = true, length = 2), stats.currentStreak)
        assertEquals(Money.euros(20), stats.maxDrawdown)
        assertEquals("Tennis", stats.bySport.first().label)
    }

    @Test
    fun `kelly stakes nothing without an edge and more with a winning record`() {
        val balance = Money.euros(1000)
        assertEquals(Money.ZERO, KellyAdvisor.advise(2.0, balance, 0.25, emptyList()).stake)

        val winningRecord = (1L..20L).map { bet(it, 1.8, Money.euros(10), BetStatus.WON) }
        val advice = KellyAdvisor.advise(1.8, balance, 0.25, winningRecord)

        assertTrue(advice.stake.isPositive)
        assertTrue(advice.probability > 1 / 1.8)
    }

    @Test
    fun `rules detect oversized stakes and betting through a losing streak`() {
        val bets =
            listOf(
                bet(1, 2.0, Money.euros(10), BetStatus.LOST),
                bet(2, 2.0, Money.euros(10), BetStatus.LOST),
                bet(3, 2.0, Money.euros(50), BetStatus.OPEN),
            )
        val context = RuleContext(bets, mapOf(1L to Money.euros(1000)), activeMontantes = 2, now = T0.plusSeconds(86_400))

        assertEquals(false, RuleEvaluator.isRespected(DisciplineRule(1, RuleKind.MAX_STAKE_PCT, 3), context))
        assertEquals(true, RuleEvaluator.isRespected(DisciplineRule(2, RuleKind.MAX_STAKE_PCT, 5), context))
        assertEquals(false, RuleEvaluator.isRespected(DisciplineRule(3, RuleKind.PAUSE_AFTER_LOSSES, 2), context))
        assertEquals(false, RuleEvaluator.isRespected(DisciplineRule(4, RuleKind.SINGLE_ACTIVE_MONTANTE, 0), context))
    }
}
