package fr.bergamof.betgamof.domain

import java.time.Instant

enum class BetType { SIMPLE, COMBINE, SYSTEME }

enum class BetStatus { OPEN, WON, LOST, VOID, CASHOUT }

data class Selection(
    val eventId: String?,
    val eventName: String,
    val sport: String,
    val competition: String,
    val market: String,
    val pick: String,
    val odds: Double,
) {
    init {
        require(eventName.isNotBlank()) { "L'événement est obligatoire" }
        require(pick.isNotBlank()) { "La sélection est obligatoire" }
        require(odds > 1.0) { "Une cote doit être supérieure à 1" }
    }
}

data class NewBet(
    val bankrollId: Long,
    val montanteId: Long?,
    val type: BetType,
    val bookmaker: String,
    val stake: Money,
    val odds: Double,
    val startsAt: Instant,
    val selections: List<Selection>,
) {
    init {
        require(stake.isPositive) { "La mise doit être positive" }
        require(odds > 1.0) { "La cote doit être supérieure à 1" }
        require(bookmaker.isNotBlank()) { "Le bookmaker est obligatoire" }
        require(selections.isNotEmpty()) { "Un pari contient au moins une sélection" }
        require(type != BetType.SIMPLE || selections.size == 1) { "Un pari simple contient une seule sélection" }
        require(type == BetType.SIMPLE || selections.size >= 2) { "Un combiné ou un système contient plusieurs sélections" }
    }
}

data class Bet(
    val id: Long,
    val bankrollId: Long,
    val montanteId: Long?,
    val type: BetType,
    val bookmaker: String,
    val stake: Money,
    val odds: Double,
    val status: BetStatus,
    val cashout: Money?,
    val selections: List<Selection>,
    val placedAt: Instant,
    val startsAt: Instant,
    val settledAt: Instant?,
) {
    val isOpen get() = status == BetStatus.OPEN

    val isSettled get() = !isOpen

    /** Won or lost: the outcomes that count for hit rate and streaks. */
    val isDecided get() = status == BetStatus.WON || status == BetStatus.LOST

    val sport get() = selections.first().sport

    val market get() = if (type == BetType.SIMPLE) selections.first().market else "Combiné"

    val potentialReturn get() = stake * odds

    val profit: Money
        get() =
            when (status) {
                BetStatus.WON -> stake * (odds - 1)
                BetStatus.LOST -> -stake
                BetStatus.CASHOUT -> (cashout ?: Money.ZERO) - stake
                BetStatus.OPEN, BetStatus.VOID -> Money.ZERO
            }
}

data class Settlement(
    val status: BetStatus,
    val cashout: Money?,
) {
    init {
        require(status != BetStatus.OPEN) { "Un règlement ne peut pas remettre le pari en cours" }
        require(status != BetStatus.CASHOUT || cashout != null) { "Le montant du cash-out est obligatoire" }
    }
}
