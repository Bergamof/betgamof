package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.model.BankrollView
import fr.bergamof.betgamof.application.model.BetView
import fr.bergamof.betgamof.application.model.KellyView
import fr.bergamof.betgamof.application.model.MontantePlanView
import fr.bergamof.betgamof.application.model.MontanteView
import fr.bergamof.betgamof.application.model.RuleView
import fr.bergamof.betgamof.application.model.SportEventView
import fr.bergamof.betgamof.application.model.StatsView
import fr.bergamof.betgamof.application.port.inbound.AddRuleCommand
import fr.bergamof.betgamof.application.port.inbound.AmendBetCommand
import fr.bergamof.betgamof.application.port.inbound.BankrollCommand
import fr.bergamof.betgamof.application.port.inbound.BankrollUseCases
import fr.bergamof.betgamof.application.port.inbound.BetFilter
import fr.bergamof.betgamof.application.port.inbound.BetUseCases
import fr.bergamof.betgamof.application.port.inbound.EventUseCases
import fr.bergamof.betgamof.application.port.inbound.InsightUseCases
import fr.bergamof.betgamof.application.port.inbound.MontanteUseCases
import fr.bergamof.betgamof.application.port.inbound.PlaceBetCommand
import fr.bergamof.betgamof.application.port.inbound.SettleBetCommand
import fr.bergamof.betgamof.application.port.inbound.StartMontanteCommand
import fr.bergamof.betgamof.application.port.inbound.StatsPeriod
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.BetId
import fr.bergamof.betgamof.domain.MontanteId
import fr.bergamof.betgamof.domain.RuleId

// Hand-written fakes of the driving ports: they record what the adapter passed and return canned
// read models, or throw [failure] to exercise the error contract.

/** Everything the fakes received, in call order. */
class Calls {
    val received = mutableListOf<Any?>()
    var failure: Throwable? = null

    fun <T> answer(
        vararg arguments: Any?,
        result: () -> T,
    ): T {
        received.addAll(arguments)
        failure?.let { throw it }
        return result()
    }

    inline fun <reified T> last(): T = received.filterIsInstance<T>().last()
}

class FakeBankrolls(
    private val calls: Calls,
) : BankrollUseCases {
    override fun list() = calls.answer { listOf(sampleBankroll) }

    override fun get(id: BankrollId) = calls.answer(id) { sampleBankroll }

    override fun create(command: BankrollCommand) = calls.answer(command) { sampleBankroll }

    override fun update(
        id: BankrollId,
        command: BankrollCommand,
    ): BankrollView = calls.answer(id, command) { sampleBankroll }

    override fun delete(id: BankrollId) = calls.answer(id) { }

    override fun adviseStake(
        id: BankrollId,
        odds: Double,
    ): KellyView = calls.answer(id, odds) { sampleKelly }
}

class FakeBets(
    private val calls: Calls,
) : BetUseCases {
    override fun list(filter: BetFilter) = calls.answer(filter) { listOf(simpleBet, combinedBet) }

    override fun get(id: BetId) = calls.answer(id) { simpleBet }

    override fun place(command: PlaceBetCommand) = calls.answer(command) { simpleBet }

    override fun amend(
        id: BetId,
        command: AmendBetCommand,
    ): BetView = calls.answer(id, command) { simpleBet }

    override fun settle(
        id: BetId,
        command: SettleBetCommand,
    ): BetView = calls.answer(id, command) { simpleBet }

    override fun delete(id: BetId) = calls.answer(id) { }
}

class FakeMontantes(
    private val calls: Calls,
) : MontanteUseCases {
    override fun list() = calls.answer { listOf(sampleMontante) }

    override fun get(id: MontanteId) = calls.answer(id) { sampleMontante }

    override fun preview(command: StartMontanteCommand): MontantePlanView = calls.answer(command) { samplePlan }

    override fun start(command: StartMontanteCommand): MontanteView = calls.answer(command) { sampleMontante }

    override fun close(id: MontanteId) = calls.answer(id) { sampleMontante }
}

class FakeInsights(
    private val calls: Calls,
) : InsightUseCases {
    override fun stats(
        period: StatsPeriod,
        bankrollId: BankrollId?,
    ): StatsView = calls.answer(period, bankrollId) { sampleStats }

    override fun rules() = calls.answer { listOf(sampleRule) }

    override fun addRule(command: AddRuleCommand): List<RuleView> = calls.answer(command) { listOf(sampleRule) }

    override fun deleteRule(id: RuleId) = calls.answer(id) { }
}

class FakeEvents(
    private val calls: Calls,
) : EventUseCases {
    override fun upcoming(query: String?): List<SportEventView> = calls.answer(query) { listOf(sampleEvent) }
}
