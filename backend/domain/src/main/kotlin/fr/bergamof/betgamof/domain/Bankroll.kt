package fr.bergamof.betgamof.domain

import java.time.Instant

/** Colour the bettor gives a bankroll to tell it apart. */
enum class BankrollColor { GAZON, CIEL, CITRON, ORANGE, BRIQUE }

@ConsistentCopyVisibility
data class BankrollSettings private constructor(
    val name: String,
    val color: BankrollColor,
    val initialBalance: Money,
    val stopLoss: Money?,
    val kellyFraction: Double,
    val fixedStake: Money?,
) {
    init {
        ensureValid(name.isNotBlank()) { "Le nom de la bankroll est obligatoire" }
        ensureValid(initialBalance.cents >= 0) { "Le solde initial doit être positif" }
        ensureValid(kellyFraction in 0.0..1.0) { "La fraction de Kelly doit être comprise entre 0 et 1" }
        ensureValid(fixedStake == null || fixedStake.isPositive) { "La mise fixe doit être positive" }
    }

    companion object {
        /** Quarter Kelly: a common compromise between growth and variance. */
        const val DEFAULT_KELLY_FRACTION = 0.25

        @Suppress("LongParameterList") // Mirrors the data class constructor.
        fun of(
            name: String,
            color: BankrollColor,
            initialBalance: Money,
            stopLoss: Money? = null,
            kellyFraction: Double = DEFAULT_KELLY_FRACTION,
            fixedStake: Money? = null,
        ) = BankrollSettings(name.trim(), color, initialBalance, stopLoss, kellyFraction, fixedStake)
    }
}

data class Bankroll(
    val id: BankrollId,
    val settings: BankrollSettings,
    val createdAt: Instant,
) {
    fun reconfigure(settings: BankrollSettings) = copy(settings = settings)

    companion object {
        fun open(
            settings: BankrollSettings,
            at: Instant,
        ) = Bankroll(BankrollId.NEW, settings, at)
    }
}
