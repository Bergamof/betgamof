package fr.bergamof.betgamof.domain

import java.time.Instant

enum class BetType { SIMPLE, COMBINE, SYSTEME }

enum class BetStatus { OPEN, WON, LOST, VOID, CASHOUT }

@ConsistentCopyVisibility
data class Selection private constructor(
    val eventId: String?,
    val eventName: String,
    val sport: String,
    val competition: String,
    val market: String,
    val pick: String,
    val odds: Double,
) {
    init {
        ensureValid(eventName.isNotBlank()) { "L'événement est obligatoire" }
        ensureValid(pick.isNotBlank()) { "La sélection est obligatoire" }
        ensureValid(odds > 1.0) { "Une cote doit être supérieure à 1" }
    }

    companion object {
        @Suppress("LongParameterList") // Mirrors the data class constructor.
        fun of(
            eventId: String?,
            eventName: String,
            sport: String,
            competition: String,
            market: String,
            pick: String,
            odds: Double,
        ) = Selection(
            eventId?.trim()?.ifEmpty { null },
            eventName.trim(),
            sport.trim(),
            competition.trim(),
            market.trim(),
            pick.trim(),
            odds,
        )
    }
}

/** A bet about to be placed: validated, not yet part of the history. */
@ConsistentCopyVisibility
data class NewBet private constructor(
    val bankrollId: BankrollId,
    val type: BetType,
    val bookmaker: String,
    val stake: Money,
    val odds: Double,
    val startsAt: Instant,
    val selections: List<Selection>,
) {
    init {
        validateTerms(stake, odds, bookmaker)
        ensureValid(selections.isNotEmpty()) { "Un pari contient au moins une sélection" }
        ensureValid(type != BetType.SIMPLE || selections.size == 1) { "Un pari simple contient une seule sélection" }
        ensureValid(type == BetType.SIMPLE || selections.size >= 2) { "Un combiné ou un système contient plusieurs sélections" }
    }

    companion object {
        @Suppress("LongParameterList") // Mirrors the data class constructor.
        fun of(
            bankrollId: BankrollId,
            type: BetType,
            bookmaker: String,
            stake: Money,
            odds: Double,
            startsAt: Instant,
            selections: List<Selection>,
        ) = NewBet(bankrollId, type, bookmaker.trim(), stake, odds, startsAt, selections)
    }
}

private fun validateTerms(
    stake: Money,
    odds: Double,
    bookmaker: String,
) {
    ensureValid(stake.isPositive) { "La mise doit être positive" }
    ensureValid(odds > 1.0) { "La cote doit être supérieure à 1" }
    ensureValid(bookmaker.isNotBlank()) { "Le bookmaker est obligatoire" }
}

data class Bet(
    val id: BetId,
    val bankrollId: BankrollId,
    val montanteId: MontanteId?,
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

    /** Market of a single bet; null for combined and system bets, which span several markets. */
    val market get() = if (type == BetType.SIMPLE) selections.first().market else null

    val potentialReturn get() = stake * odds

    val profit: Money
        get() =
            when (status) {
                BetStatus.WON -> stake * (odds - 1)
                BetStatus.LOST -> -stake
                BetStatus.CASHOUT -> (cashout ?: Money.ZERO) - stake
                BetStatus.OPEN, BetStatus.VOID -> Money.ZERO
            }

    /** Corrects the stake, odds or bookmaker of a pending bet. A palier's stake is set by its montante. */
    fun amend(
        stake: Money,
        odds: Double,
        bookmaker: String,
    ): Bet {
        ensureTransition(isOpen) { "Seul un pari en cours peut être modifié" }
        ensureTransition(montanteId == null || stake == this.stake) { "La mise d'un palier est fixée par la montante" }
        val cleanBookmaker = bookmaker.trim()
        validateTerms(stake, odds, cleanBookmaker)
        return copy(stake = stake, odds = odds, bookmaker = cleanBookmaker)
    }

    fun settle(
        settlement: Settlement,
        at: Instant,
    ): Bet {
        ensureTransition(isOpen) { "Ce pari est déjà réglé" }
        return copy(status = settlement.status, cashout = settlement.cashout, settledAt = at)
    }

    /** A settled palier is part of its montante's history and must stay. */
    fun ensureDeletable() {
        ensureTransition(montanteId == null || isOpen) {
            "Un palier réglé fait partie de l'historique de la montante et ne peut pas être supprimé"
        }
    }

    companion object {
        /** Every bet starts pending; it is settled later. */
        fun place(
            bet: NewBet,
            placedAt: Instant,
            montanteId: MontanteId? = null,
        ) = Bet(
            id = BetId.NEW,
            bankrollId = bet.bankrollId,
            montanteId = montanteId,
            type = bet.type,
            bookmaker = bet.bookmaker,
            stake = bet.stake,
            odds = bet.odds,
            status = BetStatus.OPEN,
            cashout = null,
            selections = bet.selections,
            placedAt = placedAt,
            startsAt = bet.startsAt,
            settledAt = null,
        )
    }
}

data class Settlement(
    val status: BetStatus,
    val cashout: Money?,
) {
    init {
        ensureValid(status != BetStatus.OPEN) { "Un règlement ne peut pas remettre le pari en cours" }
        ensureValid(status != BetStatus.CASHOUT || cashout != null) { "Le montant du cash-out est obligatoire" }
        ensureValid(cashout == null || cashout.cents >= 0) { "Le montant du cash-out doit être positif" }
    }
}
