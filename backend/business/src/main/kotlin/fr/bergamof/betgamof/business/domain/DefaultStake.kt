package fr.bergamof.betgamof.business.domain

private const val MAX_PERCENT = 100.0

/** Stake suggested when betting on a bankroll: a fixed amount, or a share of its balance at the time of the bet. */
sealed interface DefaultStake {
    data class Amount(
        val amount: Money,
    ) : DefaultStake {
        init {
            require(amount.isPositive) { "La mise par défaut doit être positive" }
        }
    }

    data class Percent(
        val percent: Double,
    ) : DefaultStake {
        init {
            require(percent > 0.0 && percent <= MAX_PERCENT) { "Le pourcentage de mise doit être compris entre 0 et 100" }
        }
    }
}
