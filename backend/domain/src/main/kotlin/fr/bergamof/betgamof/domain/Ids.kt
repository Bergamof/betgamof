package fr.bergamof.betgamof.domain

// Typed identifiers: a bet id cannot be passed where a bankroll id is expected.
// NEW marks an aggregate that has not been stored yet; the repository assigns the real id.

@JvmInline
value class BankrollId(
    val value: Long,
) {
    companion object {
        val NEW = BankrollId(0)
    }
}

@JvmInline
value class BetId(
    val value: Long,
) {
    companion object {
        val NEW = BetId(0)
    }
}

@JvmInline
value class MontanteId(
    val value: Long,
) {
    companion object {
        val NEW = MontanteId(0)
    }
}

@JvmInline
value class RuleId(
    val value: Long,
) {
    companion object {
        val NEW = RuleId(0)
    }
}
