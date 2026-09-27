package fr.bergamof.betgamof.business.service

import fr.bergamof.betgamof.business.NotFoundException
import fr.bergamof.betgamof.business.domain.Bet
import fr.bergamof.betgamof.business.domain.DisciplineRule
import fr.bergamof.betgamof.business.domain.NewRule
import fr.bergamof.betgamof.business.domain.PageRequest
import fr.bergamof.betgamof.business.domain.Period
import fr.bergamof.betgamof.business.domain.RuleContext
import fr.bergamof.betgamof.business.domain.RuleEvaluator
import fr.bergamof.betgamof.business.domain.RuleKind
import fr.bergamof.betgamof.business.domain.StatsCalculator
import fr.bergamof.betgamof.business.domain.sum
import fr.bergamof.betgamof.business.model.MontanteSummaryView
import fr.bergamof.betgamof.business.model.ProfitPointView
import fr.bergamof.betgamof.business.model.RuleView
import fr.bergamof.betgamof.business.model.StatsView
import fr.bergamof.betgamof.business.model.StreakView
import fr.bergamof.betgamof.business.port.inbound.InsightUseCases
import fr.bergamof.betgamof.business.port.inbound.StatsPeriod
import fr.bergamof.betgamof.business.port.outbound.BetCriteria
import fr.bergamof.betgamof.business.port.outbound.BetOrder
import fr.bergamof.betgamof.business.port.outbound.BetRepository
import fr.bergamof.betgamof.business.port.outbound.MontanteRepository
import fr.bergamof.betgamof.business.port.outbound.RuleRepository
import java.time.Clock
import java.time.ZoneId

/** Statistics and discipline rules: everything that reads the betting history as a whole. */
class InsightService(
    private val portfolio: Portfolio,
    private val bets: BetRepository,
    private val montantes: MontanteRepository,
    private val rules: RuleRepository,
    private val clock: Clock,
    private val zone: ZoneId,
) : InsightUseCases {
    override fun stats(
        period: StatsPeriod,
        bankrollId: Long?,
    ): StatsView {
        val bankroll = bankrollId?.let(portfolio::bankroll)
        val range = period.toPeriod(clock.instant())
        val periodBets = bets.find(BetCriteria(bankrollId = bankrollId, placed = range))
        val periodMontantes =
            (bankroll?.let { montantes.findByBankroll(it.id) } ?: montantes.findAll()).filter { it.createdAt in range }
        val totalBalance =
            if (bankroll != null) {
                portfolio.position(bankroll).balance
            } else {
                portfolio
                    .positions()
                    .values
                    .map { it.balance }
                    .sum()
            }
        val stats = StatsCalculator.compute(periodBets, portfolio.states(periodMontantes).map { it.second }, zone)
        return StatsView(
            settledCount = stats.settledCount,
            openCount = stats.openCount,
            staked = stats.staked,
            profit = stats.profit,
            yield = stats.yield,
            won = stats.won,
            lost = stats.lost,
            hitRate = stats.hitRate,
            averageOdds = stats.averageOdds,
            breakEvenOdds = stats.breakEvenOdds,
            averageStake = stats.averageStake,
            averageStakeShare = if (totalBalance.isPositive) stats.averageStake / totalBalance else 0.0,
            bestWinStreak = stats.bestWinStreak,
            currentStreak = stats.currentStreak?.let { StreakView(it.won, it.length) },
            maxDrawdown = stats.maxDrawdown,
            firstBetAt = stats.firstBetAt,
            profitCurve = stats.profitCurve.map { ProfitPointView(it.at, it.cumulativeProfit) },
            bySport = stats.bySport.map { it.toView() },
            byOddsRange = stats.byOddsRange.map { it.toView() },
            byMarket = stats.byMarket.map { it.toView() },
            lateNight = stats.lateNight.toView(),
            placedDayBefore = stats.placedDayBefore.toView(),
            montantes =
                stats.montantes.let {
                    MontanteSummaryView(
                        it.launched,
                        it.succeeded,
                        it.broken,
                        it.active,
                        it.closed,
                        it.averagePaliersReached,
                        it.securedCoverage,
                    )
                },
        )
    }

    override fun rules(): List<RuleView> {
        val activeRules = rules.findAll()
        val now = clock.instant()
        val context =
            RuleContext(
                bets = ruleBets(activeRules, RuleEvaluator.window(now)),
                balances = portfolio.positions().mapValues { it.value.balance },
                activeMontantes = portfolio.allStates().count { it.second.isActive },
                now = now,
            )
        return activeRules.map { RuleView(it.id, it.kind, it.param, RuleEvaluator.isRespected(it, context)) }
    }

    /**
     * Bets placed or settled in the window, plus the decided bets settled just before it that a losing
     * streak watched by a rule may still include (see [RuleContext.bets]).
     */
    private fun ruleBets(
        activeRules: List<DisciplineRule>,
        window: Period,
    ): List<Bet> {
        val longestStreak = activeRules.filter { it.kind == RuleKind.PAUSE_AFTER_LOSSES }.maxOfOrNull { it.param }
        val streakStart =
            longestStreak
                ?.let {
                    bets.find(
                        BetCriteria(
                            decidedOnly = true,
                            settled = Period(until = window.from),
                            order = BetOrder.LATEST_SETTLED,
                            page = PageRequest(it),
                        ),
                    )
                }.orEmpty()
        val placed = bets.find(BetCriteria(placed = window))
        val settled = bets.find(BetCriteria(settled = window))
        return (streakStart + placed + settled).distinctBy { it.id }
    }

    override fun addRule(rule: NewRule): List<RuleView> {
        rules.create(rule)
        return rules()
    }

    override fun deleteRule(id: Long) {
        if (!rules.delete(id)) throw NotFoundException("Règle $id introuvable")
    }
}
