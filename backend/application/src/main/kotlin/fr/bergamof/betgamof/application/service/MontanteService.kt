package fr.bergamof.betgamof.application.service

import fr.bergamof.betgamof.application.model.MontantePlanView
import fr.bergamof.betgamof.application.model.MontanteView
import fr.bergamof.betgamof.application.model.PlannedStepView
import fr.bergamof.betgamof.application.port.inbound.MontanteUseCases
import fr.bergamof.betgamof.application.port.inbound.StartMontanteCommand
import fr.bergamof.betgamof.application.port.outbound.MontanteRepository
import fr.bergamof.betgamof.application.port.outbound.TransactionRunner
import fr.bergamof.betgamof.domain.Montante
import fr.bergamof.betgamof.domain.MontanteConfig
import fr.bergamof.betgamof.domain.MontanteId
import fr.bergamof.betgamof.domain.MontantePlanner
import java.time.Clock

class MontanteService(
    private val portfolio: PortfolioLoader,
    private val repository: MontanteRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) : MontanteUseCases {
    override fun list(): List<MontanteView> =
        transactions.inTransaction {
            val snapshot = portfolio.load()
            snapshot.montantes
                .sortedWith(compareBy({ !it.state.isActive }, { -it.id.value }))
                .map { it.toView(snapshot) }
        }

    override fun get(id: MontanteId): MontanteView =
        transactions.inTransaction {
            val snapshot = portfolio.load()
            snapshot.requireMontante(id).toView(snapshot)
        }

    override fun preview(command: StartMontanteCommand): MontantePlanView =
        transactions.inTransaction {
            val config = command.toConfig()
            val snapshot = portfolio.load()
            snapshot.requireBankroll(config.bankrollId)
            val balance = snapshot.position(config.bankrollId).balance
            val plan = MontantePlanner.plan(config)
            MontantePlanView(
                target = plan.target,
                plannedSteps = plan.plannedSteps,
                steps = plan.steps.map { PlannedStepView(it.number, it.stake, it.secured, it.capitalAfter) },
                successProbability = plan.successProbability,
                bankrollBalance = balance,
                bankrollBalanceAfterLaunch = if (config.excludeStake) balance - config.startCapital else balance,
            )
        }

    override fun start(command: StartMontanteCommand): MontanteView =
        transactions.inTransaction {
            val config = command.toConfig()
            val snapshot = portfolio.load()
            snapshot.requireBankroll(config.bankrollId)
            val montante = repository.add(Montante.start(config, snapshot.position(config.bankrollId), clock.instant()))
            get(montante.id)
        }

    override fun close(id: MontanteId): MontanteView =
        transactions.inTransaction {
            repository.save(portfolio.load().requireMontante(id).close(clock.instant()))
            get(id)
        }

    private fun StartMontanteCommand.toConfig() =
        MontanteConfig.of(
            name = name,
            bankrollId = bankrollId,
            startCapital = startCapital,
            targetOdds = targetOdds,
            mode = mode,
            targetMultiplier = targetMultiplier,
            stepCount = stepCount,
            excludeStake = excludeStake,
            securePct = securePct,
            relancesAllowed = relancesAllowed,
        )
}
