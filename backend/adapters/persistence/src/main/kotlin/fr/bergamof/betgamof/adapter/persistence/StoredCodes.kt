package fr.bergamof.betgamof.adapter.persistence

import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.MontanteMode
import fr.bergamof.betgamof.domain.RuleKind
import kotlin.enums.EnumEntries

// Codes stored in the database for the domain enums. They are part of the schema, not of the domain:
// renaming a Kotlin constant must not change them, or existing databases would no longer be readable.
// The codes are the constant names the first schema version stored, and must stay so.

/** Two-way mapping between the constants of an enum and the codes stored in [column]. */
internal class StoredCodes<E : Enum<E>>(
    private val column: String,
    entries: EnumEntries<E>,
    private val code: (E) -> String,
) {
    private val byCode = entries.associateBy(code)

    init {
        check(byCode.size == entries.size) { "Two constants share a stored code in $column" }
    }

    val codes get() = byCode.keys

    fun encode(value: E): String = code(value)

    fun decode(stored: String): E = byCode[stored] ?: error("Unknown code '$stored' in column $column")
}

internal val bankrollColorCodes =
    StoredCodes("bankrolls.color", BankrollColor.entries) {
        when (it) {
            BankrollColor.GAZON -> "GAZON"
            BankrollColor.CIEL -> "CIEL"
            BankrollColor.CITRON -> "CITRON"
            BankrollColor.ORANGE -> "ORANGE"
            BankrollColor.BRIQUE -> "BRIQUE"
        }
    }

internal val betTypeCodes =
    StoredCodes("bets.type", BetType.entries) {
        when (it) {
            BetType.SIMPLE -> "SIMPLE"
            BetType.COMBINE -> "COMBINE"
            BetType.SYSTEME -> "SYSTEME"
        }
    }

internal val betStatusCodes =
    StoredCodes("bets.status", BetStatus.entries) {
        when (it) {
            BetStatus.OPEN -> "OPEN"
            BetStatus.WON -> "WON"
            BetStatus.LOST -> "LOST"
            BetStatus.VOID -> "VOID"
            BetStatus.CASHOUT -> "CASHOUT"
        }
    }

internal val montanteModeCodes =
    StoredCodes("montantes.mode", MontanteMode.entries) {
        when (it) {
            MontanteMode.OBJECTIVE -> "OBJECTIVE"
            MontanteMode.STEPS -> "STEPS"
            MontanteMode.FREE -> "FREE"
        }
    }

internal val ruleKindCodes =
    StoredCodes("discipline_rules.kind", RuleKind.entries) {
        when (it) {
            RuleKind.MAX_STAKE_PCT -> "MAX_STAKE_PCT"
            RuleKind.PAUSE_AFTER_LOSSES -> "PAUSE_AFTER_LOSSES"
            RuleKind.SINGLE_ACTIVE_MONTANTE -> "SINGLE_ACTIVE_MONTANTE"
        }
    }
