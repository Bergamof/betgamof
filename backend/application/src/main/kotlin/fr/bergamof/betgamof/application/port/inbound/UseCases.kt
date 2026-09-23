package fr.bergamof.betgamof.application.port.inbound

import fr.bergamof.betgamof.application.model.BankrollView
import fr.bergamof.betgamof.application.model.BetView
import fr.bergamof.betgamof.application.model.KellyView
import fr.bergamof.betgamof.application.model.MontantePlanView
import fr.bergamof.betgamof.application.model.MontanteView
import fr.bergamof.betgamof.application.model.RuleView
import fr.bergamof.betgamof.application.model.StatsView
import fr.bergamof.betgamof.domain.BankrollSettings
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteConfig
import fr.bergamof.betgamof.domain.NewBet
import fr.bergamof.betgamof.domain.RuleKind
import fr.bergamof.betgamof.domain.Settlement
import fr.bergamof.betgamof.domain.SportEvent

// Driving ports: everything the outside world (the REST API) may ask the application.

interface BankrollUseCases {
    fun list(): List<BankrollView>

    fun get(id: Long): BankrollView

    fun create(settings: BankrollSettings): BankrollView

    fun update(
        id: Long,
        settings: BankrollSettings,
    ): BankrollView

    fun delete(id: Long)
}

data class BetFilter(
    val status: BetStatus? = null,
    val bankrollId: Long? = null,
    val sport: String? = null,
    val bookmaker: String? = null,
    val lastDays: Long? = null,
    val query: String? = null,
)

interface BetUseCases {
    fun list(filter: BetFilter): List<BetView>

    fun get(id: Long): BetView

    fun create(request: NewBet): BetView

    fun update(
        id: Long,
        stake: Money,
        odds: Double,
        bookmaker: String,
    ): BetView

    fun settle(
        id: Long,
        settlement: Settlement,
    ): BetView

    fun delete(id: Long)

    /** Stake advised by the Kelly criterion for [odds] on a bankroll. */
    fun kelly(
        bankrollId: Long,
        odds: Double,
    ): KellyView
}

interface MontanteUseCases {
    fun list(): List<MontanteView>

    fun get(id: Long): MontanteView

    fun preview(config: MontanteConfig): MontantePlanView

    fun create(config: MontanteConfig): MontanteView

    fun close(id: Long): MontanteView
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
        bankrollId: Long?,
    ): StatsView

    fun rules(): List<RuleView>

    fun addRule(
        kind: RuleKind,
        param: Int,
    ): List<RuleView>

    fun deleteRule(id: Long)
}

interface EventUseCases {
    /** Upcoming events, optionally filtered by name. */
    fun upcoming(query: String?): List<SportEvent>
}
