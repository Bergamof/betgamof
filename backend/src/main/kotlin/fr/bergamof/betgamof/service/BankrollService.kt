package fr.bergamof.betgamof.service

import fr.bergamof.betgamof.domain.BankrollSettings
import fr.bergamof.betgamof.persistence.BankrollRepository
import java.time.Clock

class BankrollService(
    private val portfolio: PortfolioLoader,
    private val repository: BankrollRepository,
    private val clock: Clock,
) {
    fun list(): List<BankrollView> {
        val snapshot = portfolio.load()
        return snapshot.bankrolls.map { it.toView(snapshot.position(it.id), snapshot.bets) }
    }

    fun get(id: Long): BankrollView {
        val snapshot = portfolio.load()
        return snapshot.bankroll(id).toView(snapshot.position(id), snapshot.bets)
    }

    fun create(settings: BankrollSettings): BankrollView = get(repository.create(settings, clock.instant()))

    fun update(
        id: Long,
        settings: BankrollSettings,
    ): BankrollView {
        if (!repository.update(id, settings)) throw NotFoundException("Bankroll $id introuvable")
        return get(id)
    }

    fun delete(id: Long) {
        val snapshot = portfolio.load()
        snapshot.bankroll(id)
        val inUse =
            snapshot.bets.any { it.bankrollId == id } || snapshot.montantes.any { (montante, _) -> montante.config.bankrollId == id }
        if (inUse) throw ConflictException("Cette bankroll contient des paris ou des montantes : elle ne peut pas être supprimée")
        repository.delete(id)
    }
}
