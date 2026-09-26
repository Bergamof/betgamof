package fr.bergamof.betgamof.domain

/** Everything the bettor owns at a given moment, with derived montante states and bankroll positions. */
class Portfolio(
    val bankrolls: List<Bankroll>,
    val bets: List<Bet>,
    montantes: List<Montante>,
) {
    val montantes: List<TrackedMontante> = montantes.map { TrackedMontante.of(it, bets) }

    val positions: Map<BankrollId, BankrollPosition> by lazy {
        bankrolls.associate { it.id to BankrollLedger.position(it, bets, this.montantes) }
    }

    fun bankroll(id: BankrollId) = bankrolls.firstOrNull { it.id == id }

    fun montante(id: MontanteId) = montantes.firstOrNull { it.id == id }

    fun bet(id: BetId) = bets.firstOrNull { it.id == id }

    fun position(id: BankrollId) = positions.getValue(id)

    /** Sum of the balances of [bankrollId], or of every bankroll when null. */
    fun totalBalance(bankrollId: BankrollId? = null) =
        positions
            .filterKeys { bankrollId == null || it == bankrollId }
            .values
            .map { it.balance }
            .sum()

    /** A bankroll with history cannot disappear: its bets and montantes would lose their home. */
    fun ensureBankrollDeletable(id: BankrollId) {
        val inUse = bets.any { it.bankrollId == id } || montantes.any { it.config.bankrollId == id }
        ensureTransition(!inUse) { "Cette bankroll contient des paris ou des montantes : elle ne peut pas être supprimée" }
    }
}
