package fr.bergamof.betgamof.api

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
import fr.bergamof.betgamof.service.IsoInstant
import kotlinx.serialization.Serializable

/** Quarter Kelly: a common compromise between growth and variance. */
private const val DEFAULT_KELLY_FRACTION = 0.25

@Serializable
data class BankrollRequest(
    val name: String,
    val color: BankrollColor,
    val initialBalance: Money,
    val stopLoss: Money? = null,
    val kellyFraction: Double = DEFAULT_KELLY_FRACTION,
    val fixedStake: Money? = null,
) {
    fun toSettings() = BankrollSettings(name, color, initialBalance, stopLoss, kellyFraction, fixedStake)
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
    val stake: Money,
    val odds: Double,
    val startsAt: IsoInstant,
    val selections: List<SelectionRequest>,
) {
    fun toNewBet() = NewBet(bankrollId, montanteId, type, bookmaker, stake, odds, startsAt, selections.map { it.toSelection() })
}

@Serializable
data class BetUpdateRequest(
    val stake: Money,
    val odds: Double,
    val bookmaker: String,
)

@Serializable
data class SettlementRequest(
    val status: BetStatus,
    val cashout: Money? = null,
) {
    fun toSettlement() = Settlement(status, cashout)
}

@Serializable
data class MontanteRequest(
    val name: String,
    val bankrollId: Long,
    val startCapital: Money,
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
            startCapital,
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

@Serializable
data class ErrorResponse(
    val message: String,
)
