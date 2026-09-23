package fr.bergamof.betgamof.application.service

import fr.bergamof.betgamof.application.ConflictException
import fr.bergamof.betgamof.application.NotFoundException
import fr.bergamof.betgamof.application.model.BankrollView
import fr.bergamof.betgamof.application.port.inbound.BankrollUseCases
import fr.bergamof.betgamof.application.port.outbound.BankrollRepository
import fr.bergamof.betgamof.domain.BankrollSettings
import java.time.Clock

class BankrollService(
    private val portfolio: PortfolioLoader,
    private val repository: BankrollRepository,
    private val clock: Clock,
) : BankrollUseCases {
    override fun list(): List<BankrollView> {
        val snapshot = portfolio.load()
        return snapshot.bankrolls.map { it.toView(snapshot.position(it.id), snapshot.bets) }
    }

    override fun get(id: Long): BankrollView {
        val snapshot = portfolio.load()
        return snapshot.bankroll(id).toView(snapshot.position(id), snapshot.bets)
    }

    override fun create(settings: BankrollSettings): BankrollView = get(repository.create(settings, clock.instant()))

    override fun update(
        id: Long,
        settings: BankrollSettings,
    ): BankrollView {
        if (!repository.update(id, settings)) throw NotFoundException("Bankroll $id introuvable")
        return get(id)
    }

    override fun delete(id: Long) {
        val snapshot = portfolio.load()
        snapshot.bankroll(id)
        val inUse =
            snapshot.bets.any { it.bankrollId == id } || snapshot.montantes.any { (montante, _) -> montante.config.bankrollId == id }
        if (inUse) throw ConflictException("Cette bankroll contient des paris ou des montantes : elle ne peut pas être supprimée")
        repository.delete(id)
    }
}
