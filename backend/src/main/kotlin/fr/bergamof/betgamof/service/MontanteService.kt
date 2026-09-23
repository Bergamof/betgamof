package fr.bergamof.betgamof.service

import fr.bergamof.betgamof.domain.MontanteConfig
import fr.bergamof.betgamof.domain.MontantePlanner
import fr.bergamof.betgamof.domain.MontanteStatus
import fr.bergamof.betgamof.persistence.MontanteRepository
import java.time.Clock

class MontanteService(
    private val portfolio: PortfolioLoader,
    private val repository: MontanteRepository,
    private val clock: Clock,
) {
    fun list(): List<MontanteView> {
        val snapshot = portfolio.load()
        return snapshot.montantes
            .sortedWith(compareBy({ it.second.status != MontanteStatus.ACTIVE }, { -it.first.id }))
            .map { (montante, state) -> montanteView(montante, state, snapshot) }
    }

    fun get(id: Long): MontanteView {
        val snapshot = portfolio.load()
        val (montante, state) = snapshot.montante(id)
        return montanteView(montante, state, snapshot)
    }

    fun preview(config: MontanteConfig): MontantePlanView {
        val snapshot = portfolio.load()
        snapshot.bankroll(config.bankrollId)
        val balance = snapshot.position(config.bankrollId).balance
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

    fun create(config: MontanteConfig): MontanteView {
        val snapshot = portfolio.load()
        snapshot.bankroll(config.bankrollId)
        if (config.excludeStake && snapshot.position(config.bankrollId).balance < config.startCapital) {
            throw ConflictException("La bankroll ne couvre pas le capital de départ de la montante")
        }
        return get(repository.create(config, clock.instant()))
    }

    fun close(id: Long): MontanteView {
        val (montante, state) = portfolio.load().montante(id)
        if (!state.isActive) throw ConflictException("La montante « ${montante.config.name} » est déjà terminée")
        if (state.openBet != null) throw ConflictException("Règle d'abord le pari du palier ${state.currentPalierNumber}")
        repository.close(id, clock.instant())
        return get(id)
    }
}
