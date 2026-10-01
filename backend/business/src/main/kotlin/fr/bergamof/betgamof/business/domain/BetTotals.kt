package fr.bergamof.betgamof.business.domain

/** Aggregates of a set of bets: what a bankroll balance needs, without keeping the bets themselves. */
data class BetTotals(
    /** Stakes of settled bets. */
    val staked: Money,
    /** Net result of settled bets. */
    val profit: Money,
    /** Stakes of pending bets. */
    val openStake: Money,
) {
    /** These totals plus one bet: lets a repository fold bets one by one without loading them all. */
    fun add(
        status: BetStatus,
        stake: Money,
        odds: Double,
        cashout: Money?,
    ) = if (status == BetStatus.OPEN) {
        copy(openStake = openStake + stake)
    } else {
        copy(staked = staked + stake, profit = profit + profitOf(status, stake, odds, cashout))
    }

    companion object {
        val ZERO = BetTotals(Money.ZERO, Money.ZERO, Money.ZERO)

        fun of(bets: List<Bet>) = bets.fold(ZERO) { totals, bet -> totals.add(bet.status, bet.stake, bet.odds, bet.cashout) }
    }
}
