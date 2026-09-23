package fr.bergamof.betgamof.domain

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
 * settled direct bets, plus each montante's effect on the bankroll.
 */
object BankrollLedger {
    fun position(
        bankroll: Bankroll,
        bets: List<Bet>,
        montantes: List<Pair<Montante, MontanteState>>,
    ): BankrollPosition {
        val directBets = bets.filter { it.bankrollId == bankroll.id && it.montanteId == null }
        val ownMontantes = montantes.filter { (montante, _) -> montante.config.bankrollId == bankroll.id }
        val settled = directBets.filter { it.isSettled }
        val directProfit = settled.map { it.profit }.sum()
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
            balance = bankroll.settings.initialBalance + directProfit + montanteEffect,
            outsideMontantes = outside,
            openStake = directBets.filter { it.isOpen }.map { it.stake }.sum(),
            staked = settled.map { it.stake }.sum(),
            profit = directProfit + montanteProfit,
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
