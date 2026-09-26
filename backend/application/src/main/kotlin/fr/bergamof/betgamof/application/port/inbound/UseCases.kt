package fr.bergamof.betgamof.application.port.inbound

import fr.bergamof.betgamof.application.model.BankrollView
import fr.bergamof.betgamof.application.model.BetView
import fr.bergamof.betgamof.application.model.KellyView
import fr.bergamof.betgamof.application.model.MontantePlanView
import fr.bergamof.betgamof.application.model.MontanteView
import fr.bergamof.betgamof.application.model.RuleView
import fr.bergamof.betgamof.application.model.SportEventView
import fr.bergamof.betgamof.application.model.StatsView
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.BetId
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.MontanteId
import fr.bergamof.betgamof.domain.RuleId

// Driving ports: everything the outside world (REST API, demo seeder) may ask the application.

interface BankrollUseCases {
    fun list(): List<BankrollView>

    fun get(id: BankrollId): BankrollView

    fun create(command: BankrollCommand): BankrollView

    fun update(
        id: BankrollId,
        command: BankrollCommand,
    ): BankrollView

    fun delete(id: BankrollId)

    /** Stake advised by the Kelly criterion for [odds] on the bankroll. */
    fun adviseStake(
        id: BankrollId,
        odds: Double,
    ): KellyView
}

data class BetFilter(
    val status: BetStatus? = null,
    val bankrollId: BankrollId? = null,
    val sport: String? = null,
    val bookmaker: String? = null,
    val lastDays: Long? = null,
    val query: String? = null,
)

interface BetUseCases {
    fun list(filter: BetFilter): List<BetView>

    fun get(id: BetId): BetView

    fun place(command: PlaceBetCommand): BetView

    fun amend(
        id: BetId,
        command: AmendBetCommand,
    ): BetView

    fun settle(
        id: BetId,
        command: SettleBetCommand,
    ): BetView

    fun delete(id: BetId)
}

interface MontanteUseCases {
    fun list(): List<MontanteView>

    fun get(id: MontanteId): MontanteView

    fun preview(command: StartMontanteCommand): MontantePlanView

    fun start(command: StartMontanteCommand): MontanteView

    fun close(id: MontanteId): MontanteView
}

enum class StatsPeriod(
    val days: Long?,
) {
    DAYS_30(30),
    MONTHS_3(90),
    ALL(null),
}

interface InsightUseCases {
    fun stats(
        period: StatsPeriod,
        bankrollId: BankrollId?,
    ): StatsView

    fun rules(): List<RuleView>

    fun addRule(command: AddRuleCommand): List<RuleView>

    fun deleteRule(id: RuleId)
}

interface EventUseCases {
    /** Upcoming events, optionally filtered by name. */
    fun upcoming(query: String?): List<SportEventView>
}
