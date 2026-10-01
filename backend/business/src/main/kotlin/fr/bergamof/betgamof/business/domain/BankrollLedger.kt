package fr.bergamof.betgamof.business.domain

data class BankrollPosition(
    val balance: Money,
    /** Capital taken out of the bankroll by active montantes that exclude their stake. */
    val outsideMontantes: Money,
    /** Stakes of pending bets placed directly on the bankroll. */
    val openStake: Money,
    val staked: Money,
    val profit: Money,
) {
    val roi get() = if (staked.isPositive) profit / staked else 0.0
}

/**
 * Computes a bankroll balance from its history rather than storing it:
 * totals of its settled direct bets, plus each montante's effect on the bankroll.
 */
object BankrollLedger {
    /** [directBets]: totals of the bets placed directly on the bankroll (outside any montante). */
    fun position(
        bankroll: Bankroll,
        directBets: BetTotals,
        montantes: List<Pair<Montante, MontanteState>>,
    ): BankrollPosition {
        val ownMontantes = montantes.filter { (montante, _) -> montante.config.bankrollId == bankroll.id }
        val montanteEffect = ownMontantes.map { (montante, state) -> montanteEffect(montante, state) }.sum()
        val montanteProfit =
            ownMontantes
                .filter { (_, state) -> !state.isActive }
                .map { (_, state) -> state.result }
                .sum()
        val outside =
            ownMontantes
                .filter { (montante, state) -> montante.config.excludeStake && state.isActive }
                .map { (_, state) -> state.engaged }
                .sum()
        return BankrollPosition(
            balance = bankroll.settings.initialBalance + directBets.profit + montanteEffect,
            outsideMontantes = outside,
            openStake = directBets.openStake,
            staked = directBets.staked,
            profit = directBets.profit + montanteProfit,
        )
    }

    /**
     * Secured gains always land on the bankroll. The montante capital stays inside the bankroll
     * unless the stake is excluded, in which case it only comes back once the montante ends.
     */
    fun montanteEffect(
        montante: Montante,
        state: MontanteState,
    ): Money {
        val capitalOnBankroll = if (montante.config.excludeStake && state.isActive) Money.ZERO else state.capital
        return state.secured - state.engaged + capitalOnBankroll
    }
}
