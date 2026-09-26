package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.port.inbound.AddRuleCommand
import fr.bergamof.betgamof.application.port.inbound.AmendBetCommand
import fr.bergamof.betgamof.application.port.inbound.BankrollCommand
import fr.bergamof.betgamof.application.port.inbound.PlaceBetCommand
import fr.bergamof.betgamof.application.port.inbound.SelectionCommand
import fr.bergamof.betgamof.application.port.inbound.SettleBetCommand
import fr.bergamof.betgamof.application.port.inbound.StartMontanteCommand
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteId
import kotlinx.serialization.Serializable

// Request bodies of the REST API. They only translate JSON into commands: validation and defaults
// belong to the application and the domain.

@Serializable
data class BankrollRequest(
    val name: String,
    val color: BankrollColorJson,
    val initialBalance: Double,
    val stopLoss: Double? = null,
    /** Absent means the domain's default fraction. */
    val kellyFraction: Double? = null,
    val fixedStake: Double? = null,
) {
    fun toCommand() =
        BankrollCommand(
            name = name,
            color = color.toDomain(),
            initialBalance = Money.euros(initialBalance),
            stopLoss = stopLoss?.let { Money.euros(it) },
            kellyFraction = kellyFraction,
            fixedStake = fixedStake?.let { Money.euros(it) },
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
    fun toCommand() = SelectionCommand(eventId, eventName, sport, competition, market, pick, odds)
}

@Serializable
data class BetRequest(
    val bankrollId: Long,
    val montanteId: Long? = null,
    val type: BetTypeJson,
    val bookmaker: String,
    val stake: Double,
    val odds: Double,
    val startsAt: JsonInstant,
    val selections: List<SelectionRequest>,
) {
    fun toCommand() =
        PlaceBetCommand(
            bankrollId = BankrollId(bankrollId),
            montanteId = montanteId?.let(::MontanteId),
            type = type.toDomain(),
            bookmaker = bookmaker,
            stake = Money.euros(stake),
            odds = odds,
            startsAt = startsAt,
            selections = selections.map { it.toCommand() },
        )
}

@Serializable
data class BetUpdateRequest(
    val stake: Double,
    val odds: Double,
    val bookmaker: String,
) {
    fun toCommand() = AmendBetCommand(Money.euros(stake), odds, bookmaker)
}

@Serializable
data class SettlementRequest(
    val status: BetStatusJson,
    val cashout: Double? = null,
) {
    fun toCommand() = SettleBetCommand(status.toDomain(), cashout?.let { Money.euros(it) })
}

@Serializable
data class MontanteRequest(
    val name: String,
    val bankrollId: Long,
    val startCapital: Double,
    val targetOdds: Double,
    val mode: MontanteModeJson,
    val targetMultiplier: Double? = null,
    val stepCount: Int? = null,
    val excludeStake: Boolean,
    val securePct: Int,
    val relancesAllowed: Int = 0,
) {
    fun toCommand() =
        StartMontanteCommand(
            name = name,
            bankrollId = BankrollId(bankrollId),
            startCapital = Money.euros(startCapital),
            targetOdds = targetOdds,
            mode = mode.toDomain(),
            targetMultiplier = targetMultiplier,
            stepCount = stepCount,
            excludeStake = excludeStake,
            securePct = securePct,
            relancesAllowed = relancesAllowed,
        )
}

@Serializable
data class RuleRequest(
    val kind: RuleKindJson,
    val param: Int = 0,
) {
    fun toCommand() = AddRuleCommand(kind.toDomain(), param)
}
