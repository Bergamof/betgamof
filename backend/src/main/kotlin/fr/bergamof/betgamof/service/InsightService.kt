package fr.bergamof.betgamof.service

import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.RuleContext
import fr.bergamof.betgamof.domain.RuleEvaluator
import fr.bergamof.betgamof.domain.RuleKind
import fr.bergamof.betgamof.domain.StatsCalculator
import fr.bergamof.betgamof.persistence.RuleRepository
import java.time.Clock
import java.time.Duration
import java.time.ZoneId

enum class StatsPeriod(
    val days: Long?,
) {
    DAYS_30(30),
    MONTHS_3(90),
    ALL(null),
}

/** Statistics and discipline rules: everything that reads the betting history as a whole. */
class InsightService(
    private val portfolio: PortfolioLoader,
    private val rules: RuleRepository,
    private val clock: Clock,
    private val zone: ZoneId,
) {
    fun stats(
        period: StatsPeriod,
        bankrollId: Long?,
    ): StatsView {
        val snapshot = portfolio.load()
        bankrollId?.let(snapshot::bankroll)
        val since = period.days?.let { clock.instant() - Duration.ofDays(it) }
        val bets = snapshot.bets.filter { (bankrollId == null || it.bankrollId == bankrollId) && (since == null || it.placedAt >= since) }
        val montantes =
            snapshot.montantes
                .filter { (montante, _) -> bankrollId == null || montante.config.bankrollId == bankrollId }
                .filter { (montante, _) -> since == null || montante.createdAt >= since }
                .map { it.second }
        val totalBalance =
            snapshot.positions
                .filterKeys { bankrollId == null || it == bankrollId }
                .values
                .map { it.balance }
                .fold(Money.ZERO, Money::plus)
        val stats = StatsCalculator.compute(bets, montantes, zone)
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

    fun rules(): List<RuleView> {
        val snapshot = portfolio.load()
        val context =
            RuleContext(
                bets = snapshot.bets,
                balances = snapshot.positions.mapValues { it.value.balance },
                activeMontantes = snapshot.montantes.count { it.second.isActive },
                now = clock.instant(),
            )
        return rules.findAll().map { RuleView(it.id, it.kind, it.param, RuleEvaluator.isRespected(it, context)) }
    }

    fun addRule(
        kind: RuleKind,
        param: Int,
    ): List<RuleView> {
        require(kind == RuleKind.SINGLE_ACTIVE_MONTANTE || param > 0) { "Le paramètre de la règle doit être positif" }
        rules.create(kind, param)
        return rules()
    }

    fun deleteRule(id: Long) {
        if (!rules.delete(id)) throw NotFoundException("Règle $id introuvable")
    }
}
