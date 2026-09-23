package fr.bergamof.betgamof.domain

import java.time.Instant

/** Palette keys understood by the frontend (see `frontend/src/lib/theme.ts`). */
enum class BankrollColor { GAZON, CIEL, CITRON, ORANGE, BRIQUE }

data class BankrollSettings(
    val name: String,
    val color: BankrollColor,
    val initialBalance: Money,
    val stopLoss: Money?,
    val kellyFraction: Double,
    val fixedStake: Money?,
) {
    init {
        require(name.isNotBlank()) { "Le nom de la bankroll est obligatoire" }
        require(initialBalance.cents >= 0) { "Le solde initial doit être positif" }
        require(kellyFraction in 0.0..1.0) { "La fraction de Kelly doit être comprise entre 0 et 1" }
        require(fixedStake == null || fixedStake.isPositive) { "La mise fixe doit être positive" }
    }
}

data class Bankroll(
    val id: Long,
    val settings: BankrollSettings,
    val createdAt: Instant,
)
