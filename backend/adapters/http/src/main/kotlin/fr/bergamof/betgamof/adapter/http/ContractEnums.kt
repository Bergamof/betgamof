package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.port.inbound.StatsPeriod
import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.MontanteMode
import fr.bergamof.betgamof.domain.MontanteStatus
import fr.bergamof.betgamof.domain.RuleKind
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Enums of the public JSON contract. Their spellings are part of the API (frontend/src/lib/api/types.ts)
// and are mapped explicitly to the domain, so renaming a domain constant cannot break the API.

// Serial names keep the contract type names (not the Kotlin class names) in error messages.
@Serializable
@SerialName("BankrollColor")
enum class BankrollColorJson { GAZON, CIEL, CITRON, ORANGE, BRIQUE }

@Serializable
@SerialName("MontanteMode")
enum class MontanteModeJson { OBJECTIVE, STEPS, FREE }

@Serializable
@SerialName("MontanteStatus")
enum class MontanteStatusJson { ACTIVE, SUCCEEDED, BROKEN, CLOSED }

@Serializable
@SerialName("RuleKind")
enum class RuleKindJson { MAX_STAKE_PCT, PAUSE_AFTER_LOSSES, SINGLE_ACTIVE_MONTANTE }

/** Values of the `period` query parameter of `GET /stats`. */
@Serializable
@SerialName("StatsPeriod")
enum class StatsPeriodJson { DAYS_30, MONTHS_3, ALL }

fun BankrollColor.toJson() =
    when (this) {
        BankrollColor.GAZON -> BankrollColorJson.GAZON
        BankrollColor.CIEL -> BankrollColorJson.CIEL
        BankrollColor.CITRON -> BankrollColorJson.CITRON
        BankrollColor.ORANGE -> BankrollColorJson.ORANGE
        BankrollColor.BRIQUE -> BankrollColorJson.BRIQUE
    }

fun BankrollColorJson.toDomain() =
    when (this) {
        BankrollColorJson.GAZON -> BankrollColor.GAZON
        BankrollColorJson.CIEL -> BankrollColor.CIEL
        BankrollColorJson.CITRON -> BankrollColor.CITRON
        BankrollColorJson.ORANGE -> BankrollColor.ORANGE
        BankrollColorJson.BRIQUE -> BankrollColor.BRIQUE
    }

fun MontanteMode.toJson() =
    when (this) {
        MontanteMode.OBJECTIVE -> MontanteModeJson.OBJECTIVE
        MontanteMode.STEPS -> MontanteModeJson.STEPS
        MontanteMode.FREE -> MontanteModeJson.FREE
    }

fun MontanteModeJson.toDomain() =
    when (this) {
        MontanteModeJson.OBJECTIVE -> MontanteMode.OBJECTIVE
        MontanteModeJson.STEPS -> MontanteMode.STEPS
        MontanteModeJson.FREE -> MontanteMode.FREE
    }

/** Response only: clients never send a montante status. */
fun MontanteStatus.toJson() =
    when (this) {
        MontanteStatus.ACTIVE -> MontanteStatusJson.ACTIVE
        MontanteStatus.SUCCEEDED -> MontanteStatusJson.SUCCEEDED
        MontanteStatus.BROKEN -> MontanteStatusJson.BROKEN
        MontanteStatus.CLOSED -> MontanteStatusJson.CLOSED
    }

fun RuleKind.toJson() =
    when (this) {
        RuleKind.MAX_STAKE_PCT -> RuleKindJson.MAX_STAKE_PCT
        RuleKind.PAUSE_AFTER_LOSSES -> RuleKindJson.PAUSE_AFTER_LOSSES
        RuleKind.SINGLE_ACTIVE_MONTANTE -> RuleKindJson.SINGLE_ACTIVE_MONTANTE
    }

fun RuleKindJson.toDomain() =
    when (this) {
        RuleKindJson.MAX_STAKE_PCT -> RuleKind.MAX_STAKE_PCT
        RuleKindJson.PAUSE_AFTER_LOSSES -> RuleKind.PAUSE_AFTER_LOSSES
        RuleKindJson.SINGLE_ACTIVE_MONTANTE -> RuleKind.SINGLE_ACTIVE_MONTANTE
    }

/** Query only: the period is never part of a response. */
fun StatsPeriodJson.toDomain() =
    when (this) {
        StatsPeriodJson.DAYS_30 -> StatsPeriod.DAYS_30
        StatsPeriodJson.MONTHS_3 -> StatsPeriod.MONTHS_3
        StatsPeriodJson.ALL -> StatsPeriod.ALL
    }
