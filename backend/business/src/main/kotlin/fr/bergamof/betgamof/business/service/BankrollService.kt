package fr.bergamof.betgamof.business.service

import fr.bergamof.betgamof.business.ConflictException
import fr.bergamof.betgamof.business.NotFoundException
import fr.bergamof.betgamof.business.domain.Bankroll
import fr.bergamof.betgamof.business.domain.BankrollPosition
import fr.bergamof.betgamof.business.domain.BankrollSettings
import fr.bergamof.betgamof.business.model.BankrollView
import fr.bergamof.betgamof.business.port.inbound.BankrollUseCases
import fr.bergamof.betgamof.business.port.outbound.BankrollRepository
import fr.bergamof.betgamof.business.port.outbound.BetCriteria
import fr.bergamof.betgamof.business.port.outbound.BetRepository
import fr.bergamof.betgamof.business.port.outbound.MontanteRepository
import java.time.Clock

class BankrollService(
    private val portfolio: Portfolio,
    private val repository: BankrollRepository,
    private val bets: BetRepository,
    private val montantes: MontanteRepository,
    private val clock: Clock,
) : BankrollUseCases {
    override fun list(): List<BankrollView> {
        val positions = portfolio.positions()
        return repository.findAll().map { view(it, positions.getValue(it.id)) }
    }

    override fun get(id: Long): BankrollView {
        val bankroll = portfolio.bankroll(id)
        return view(bankroll, portfolio.position(bankroll))
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
        portfolio.bankroll(id)
        val inUse = bets.count(BetCriteria(bankrollId = id)) > 0 || montantes.findByBankroll(id).isNotEmpty()
        if (inUse) throw ConflictException("Cette bankroll contient des paris ou des montantes : elle ne peut pas être supprimée")
        repository.delete(id)
    }

    private fun view(
        bankroll: Bankroll,
        position: BankrollPosition,
    ) = bankroll.toView(position, bets.count(BetCriteria(bankrollId = bankroll.id)), bets.bookmakersByUsage(bankroll.id))
}
