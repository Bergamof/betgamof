package fr.bergamof.betgamof.application.service

import fr.bergamof.betgamof.application.NotFoundException
import fr.bergamof.betgamof.application.model.MontanteSummaryView
import fr.bergamof.betgamof.application.model.ProfitPointView
import fr.bergamof.betgamof.application.model.RuleView
import fr.bergamof.betgamof.application.model.StatsView
import fr.bergamof.betgamof.application.model.StreakView
import fr.bergamof.betgamof.application.port.inbound.AddRuleCommand
import fr.bergamof.betgamof.application.port.inbound.InsightUseCases
import fr.bergamof.betgamof.application.port.inbound.StatsPeriod
import fr.bergamof.betgamof.application.port.outbound.RuleRepository
import fr.bergamof.betgamof.application.port.outbound.TransactionRunner
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.DisciplineRule
import fr.bergamof.betgamof.domain.RuleContext
import fr.bergamof.betgamof.domain.RuleEvaluator
import fr.bergamof.betgamof.domain.RuleId
import fr.bergamof.betgamof.domain.Stats
import fr.bergamof.betgamof.domain.StatsCalculator
import java.time.Clock
import java.time.Duration
import java.time.ZoneId

/** Statistics and discipline rules: everything that reads the betting history as a whole. */
class InsightService(
    private val portfolio: PortfolioLoader,
    private val rules: RuleRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
    private val zone: ZoneId,
) : InsightUseCases {
    override fun stats(
        period: StatsPeriod,
        bankrollId: BankrollId?,
    ): StatsView =
        transactions.inTransaction {
            val snapshot = portfolio.load()
            bankrollId?.let(snapshot::requireBankroll)
            val since = period.days?.let { clock.instant() - Duration.ofDays(it) }
            val bets =
                snapshot.bets.filter {
                    (bankrollId == null || it.bankrollId == bankrollId) && (since == null || it.placedAt >= since)
                }
            val montantes =
                snapshot.montantes
                    .filter { bankrollId == null || it.config.bankrollId == bankrollId }
                    .filter { since == null || it.montante.createdAt >= since }
                    .map { it.state }
            StatsCalculator.compute(bets, montantes, snapshot.totalBalance(bankrollId), zone).toView()
        }

    override fun rules(): List<RuleView> =
        transactions.inTransaction {
            val snapshot = portfolio.load()
            val context =
                RuleContext(
                    bets = snapshot.bets,
                    balances = snapshot.positions.mapValues { it.value.balance },
                    activeMontantes = snapshot.montantes.count { it.state.isActive },
                    now = clock.instant(),
                )
            rules.findAll().map { RuleView(it.id, it.kind, it.param, RuleEvaluator.isRespected(it, context)) }
        }

    override fun addRule(command: AddRuleCommand): List<RuleView> =
        transactions.inTransaction {
            rules.add(DisciplineRule.define(command.kind, command.param))
            rules()
        }

    override fun deleteRule(id: RuleId) =
        transactions.inTransaction {
            rules.findById(id) ?: throw NotFoundException("Règle ${id.value} introuvable")
            rules.remove(id)
        }

    private fun Stats.toView() =
        StatsView(
            settledCount = settledCount,
            openCount = openCount,
            staked = staked,
            profit = profit,
            yield = yield,
            won = won,
            lost = lost,
            hitRate = hitRate,
            averageOdds = averageOdds,
            breakEvenOdds = breakEvenOdds,
            averageStake = averageStake,
            averageStakeShare = averageStakeShare,
            bestWinStreak = bestWinStreak,
            currentStreak = currentStreak?.let { StreakView(it.won, it.length) },
            maxDrawdown = maxDrawdown,
            firstBetAt = firstBetAt,
            profitCurve = profitCurve.map { ProfitPointView(it.at, it.cumulativeProfit) },
            bySport = bySport.map { it.toView() },
            byOddsRange = byOddsRange.map { it.toView() },
            byMarket = byMarket.map { it.toView() },
            lateNight = lateNight.toView(),
            placedDayBefore = placedDayBefore.toView(),
            montantes =
                montantes.let {
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
