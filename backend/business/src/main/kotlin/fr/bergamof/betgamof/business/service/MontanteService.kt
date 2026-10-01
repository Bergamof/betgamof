package fr.bergamof.betgamof.business.service

import fr.bergamof.betgamof.business.ConflictException
import fr.bergamof.betgamof.business.domain.MontanteConfig
import fr.bergamof.betgamof.business.domain.MontantePlanner
import fr.bergamof.betgamof.business.domain.MontanteStatus
import fr.bergamof.betgamof.business.model.MontantePlanView
import fr.bergamof.betgamof.business.model.MontanteView
import fr.bergamof.betgamof.business.model.PlannedStepView
import fr.bergamof.betgamof.business.port.inbound.MontanteUseCases
import fr.bergamof.betgamof.business.port.outbound.MontanteRepository
import java.time.Clock

class MontanteService(
    private val portfolio: Portfolio,
    private val repository: MontanteRepository,
    private val clock: Clock,
) : MontanteUseCases {
    override fun list(): List<MontanteView> {
        val bankrollNames = portfolio.bankrollNames()
        return portfolio
            .allStates()
            .sortedWith(compareBy({ it.second.status != MontanteStatus.ACTIVE }, { -it.first.id }))
            .map { (montante, state) -> montanteView(montante, state, bankrollNames.getValue(montante.config.bankrollId)) }
    }

    override fun get(id: Long): MontanteView {
        val (montante, state) = portfolio.montante(id)
        return montanteView(montante, state, portfolio.bankroll(montante.config.bankrollId).settings.name)
    }

    override fun preview(config: MontanteConfig): MontantePlanView {
        val balance = portfolio.position(portfolio.bankroll(config.bankrollId)).balance
        val plan = MontantePlanner.plan(config)
        return MontantePlanView(
            target = plan.target,
            plannedSteps = plan.plannedSteps,
            steps = plan.steps.map { PlannedStepView(it.number, it.stake, it.secured, it.capitalAfter) },
            successProbability = plan.successProbability,
            bankrollBalance = balance,
            bankrollBalanceAfterLaunch = if (config.excludeStake) balance - config.startCapital else balance,
        )
    }

    override fun create(config: MontanteConfig): MontanteView {
        val bankroll = portfolio.bankroll(config.bankrollId)
        if (config.excludeStake && portfolio.position(bankroll).balance < config.startCapital) {
            throw ConflictException("La bankroll ne couvre pas le capital de départ de la montante")
        }
        return get(repository.create(config, clock.instant()))
    }

    override fun close(id: Long): MontanteView {
        val (montante, state) = portfolio.montante(id)
        if (!state.isActive) throw ConflictException("La montante « ${montante.config.name} » est déjà terminée")
        if (state.openBet != null) throw ConflictException("Règle d'abord le pari du palier ${state.currentPalierNumber}")
        repository.close(id, clock.instant())
        return get(id)
    }
}
