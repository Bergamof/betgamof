package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BankrollSettings
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteConfig
import fr.bergamof.betgamof.domain.MontanteMode
import fr.bergamof.betgamof.domain.NewBet
import fr.bergamof.betgamof.domain.RuleKind
import fr.bergamof.betgamof.domain.Selection
import fr.bergamof.betgamof.domain.Settlement
import kotlinx.serialization.Serializable

/** Quarter Kelly: a common compromise between growth and variance. */
private const val DEFAULT_KELLY_FRACTION = 0.25

@Serializable
data class BankrollRequest(
    val name: String,
    val color: BankrollColor,
    val initialBalance: Double,
    val stopLoss: Double? = null,
    val kellyFraction: Double = DEFAULT_KELLY_FRACTION,
    val fixedStake: Double? = null,
) {
    fun toSettings() =
        BankrollSettings(
            name,
            color,
            Money.euros(initialBalance),
            stopLoss?.let {
                Money.euros(it)
            },
            kellyFraction,
            fixedStake?.let { Money.euros(it) },
        )
}

@Serializable
data class SelectionRequest(
    val eventId: String? = null,
    val eventName: String,
    val sport: String,
    val competition: String = "",
    val market: String = "",
    val pick: String,
    val odds: Double,
) {
    fun toSelection() = Selection(eventId, eventName, sport, competition, market, pick, odds)
}

@Serializable
data class BetRequest(
    val bankrollId: Long,
    val montanteId: Long? = null,
    val type: BetType,
    val bookmaker: String,
    val stake: Double,
    val odds: Double,
    val startsAt: JsonInstant,
    val selections: List<SelectionRequest>,
) {
    fun toNewBet() =
        NewBet(bankrollId, montanteId, type, bookmaker, Money.euros(stake), odds, startsAt, selections.map { it.toSelection() })
}

@Serializable
data class BetUpdateRequest(
    val stake: Double,
    val odds: Double,
    val bookmaker: String,
) {
    fun stakeMoney() = Money.euros(stake)
}

@Serializable
data class SettlementRequest(
    val status: BetStatus,
    val cashout: Double? = null,
) {
    fun toSettlement() = Settlement(status, cashout?.let { Money.euros(it) })
}

@Serializable
data class MontanteRequest(
    val name: String,
    val bankrollId: Long,
    val startCapital: Double,
    val targetOdds: Double,
    val mode: MontanteMode,
    val targetMultiplier: Double? = null,
    val stepCount: Int? = null,
    val excludeStake: Boolean,
    val securePct: Int,
    val relancesAllowed: Int = 0,
) {
    fun toConfig() =
        MontanteConfig(
            name,
            bankrollId,
            Money.euros(startCapital),
            targetOdds,
            mode,
            targetMultiplier,
            stepCount,
            excludeStake,
            securePct,
            relancesAllowed,
        )
}

@Serializable
data class RuleRequest(
    val kind: RuleKind,
    val param: Int = 0,
)
