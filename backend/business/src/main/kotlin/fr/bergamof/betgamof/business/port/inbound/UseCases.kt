package fr.bergamof.betgamof.business.port.inbound

import fr.bergamof.betgamof.business.domain.BankrollSettings
import fr.bergamof.betgamof.business.domain.BetChange
import fr.bergamof.betgamof.business.domain.BetStatus
import fr.bergamof.betgamof.business.domain.MontanteConfig
import fr.bergamof.betgamof.business.domain.NewBet
import fr.bergamof.betgamof.business.domain.NewRule
import fr.bergamof.betgamof.business.domain.PageRequest
import fr.bergamof.betgamof.business.domain.Period
import fr.bergamof.betgamof.business.domain.Settlement
import fr.bergamof.betgamof.business.domain.SportEvent
import fr.bergamof.betgamof.business.model.BankrollView
import fr.bergamof.betgamof.business.model.BetView
import fr.bergamof.betgamof.business.model.KellyView
import fr.bergamof.betgamof.business.model.MontantePlanView
import fr.bergamof.betgamof.business.model.MontanteView
import fr.bergamof.betgamof.business.model.RuleView
import fr.bergamof.betgamof.business.model.StatsView
import java.time.Duration
import java.time.Instant

// Driving ports: everything the outside world (the REST API) may ask the business.

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
    /** Without a page, every matching bet is returned. */
    val page: PageRequest? = null,
)

interface BetUseCases {
    fun list(filter: BetFilter): List<BetView>

    fun get(id: Long): BetView

    fun create(request: NewBet): BetView

    fun update(
        id: Long,
        change: BetChange,
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
    ;

    fun toPeriod(now: Instant) = days?.let { Period.last(Duration.ofDays(it), now) } ?: Period.ALL
}

interface InsightUseCases {
    fun stats(
        period: StatsPeriod,
        bankrollId: Long?,
    ): StatsView

    fun rules(): List<RuleView>

    fun addRule(rule: NewRule): List<RuleView>

    fun deleteRule(id: Long)
}

interface EventUseCases {
    /** Upcoming events, optionally filtered by name. */
    fun upcoming(query: String?): List<SportEvent>
}
