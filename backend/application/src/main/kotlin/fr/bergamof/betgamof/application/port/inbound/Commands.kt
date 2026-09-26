package fr.bergamof.betgamof.application.port.inbound

import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteId
import fr.bergamof.betgamof.domain.MontanteMode
import fr.bergamof.betgamof.domain.RuleKind
import java.time.Instant

// Input of the driving ports: raw requests, not yet validated. The use cases turn them into domain
// objects, so validation errors are raised inside the application as domain exceptions.

/** Creates or reconfigures a bankroll. A null Kelly fraction means the domain default. */
data class BankrollCommand(
    val name: String,
    val color: BankrollColor,
    val initialBalance: Money,
    val stopLoss: Money? = null,
    val kellyFraction: Double? = null,
    val fixedStake: Money? = null,
)

data class SelectionCommand(
    val eventId: String?,
    val eventName: String,
    val sport: String,
    val competition: String,
    val market: String,
    val pick: String,
    val odds: Double,
)

/** Places a bet. With a montante, the stake is replaced by the montante's capital. */
data class PlaceBetCommand(
    val bankrollId: BankrollId,
    val montanteId: MontanteId?,
    val type: BetType,
    val bookmaker: String,
    val stake: Money,
    val odds: Double,
    val startsAt: Instant,
    val selections: List<SelectionCommand>,
)

data class AmendBetCommand(
    val stake: Money,
    val odds: Double,
    val bookmaker: String,
)

data class SettleBetCommand(
    val status: BetStatus,
    val cashout: Money? = null,
)

/** Starts a montante, or previews its plan. */
data class StartMontanteCommand(
    val name: String,
    val bankrollId: BankrollId,
    val startCapital: Money,
    val targetOdds: Double,
    val mode: MontanteMode,
    val targetMultiplier: Double?,
    val stepCount: Int?,
    val excludeStake: Boolean,
    val securePct: Int,
    val relancesAllowed: Int,
)

data class AddRuleCommand(
    val kind: RuleKind,
    val param: Int,
)
