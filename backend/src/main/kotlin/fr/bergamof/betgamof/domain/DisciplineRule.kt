package fr.bergamof.betgamof.domain

import java.time.Duration
import java.time.Instant

private val RULE_WINDOW: Duration = Duration.ofDays(30)

enum class RuleKind {
    /** No single bet above `param` % of the bankroll balance. */
    MAX_STAKE_PCT,

    /** No new bet after `param` consecutive losses. */
    PAUSE_AFTER_LOSSES,

    /** At most one active montante at a time. */
    SINGLE_ACTIVE_MONTANTE,
}

data class DisciplineRule(
    val id: Long,
    val kind: RuleKind,
    val param: Int,
) {
    init {
        require(kind == RuleKind.SINGLE_ACTIVE_MONTANTE || param > 0) { "Le paramètre de la règle doit être positif" }
    }
}

data class RuleContext(
    val bets: List<Bet>,
    val balances: Map<Long, Money>,
    val activeMontantes: Int,
    val now: Instant,
)

/** Checks each rule against the last 30 days of activity. */
object RuleEvaluator {
    fun isRespected(
        rule: DisciplineRule,
        context: RuleContext,
    ): Boolean {
        val since = context.now - RULE_WINDOW
        val recent = context.bets.filter { it.placedAt >= since }
        return when (rule.kind) {
            RuleKind.MAX_STAKE_PCT -> recent.filter { it.montanteId == null }.none { exceedsShare(it, rule.param, context.balances) }
            RuleKind.PAUSE_AFTER_LOSSES -> recent.none { betPlacedDuringLossStreak(it, context.bets, rule.param) }
            RuleKind.SINGLE_ACTIVE_MONTANTE -> context.activeMontantes <= 1
        }
    }

    private fun exceedsShare(
        bet: Bet,
        maxPct: Int,
        balances: Map<Long, Money>,
    ): Boolean {
        val balance = balances[bet.bankrollId] ?: return false
        return balance.isPositive && bet.stake / balance * 100 > maxPct
    }

    private fun betPlacedDuringLossStreak(
        bet: Bet,
        allBets: List<Bet>,
        maxLosses: Int,
    ): Boolean {
        val decidedBefore =
            allBets
                .filter { it.isDecided && it.id != bet.id && requireNotNull(it.settledAt) <= bet.placedAt }
                .sortedBy { it.settledAt }
        val trailingLosses = decidedBefore.asReversed().takeWhile { it.status == BetStatus.LOST }.size
        return trailingLosses >= maxLosses
    }
}
