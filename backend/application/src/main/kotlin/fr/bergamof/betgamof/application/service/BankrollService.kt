package fr.bergamof.betgamof.application.service

import fr.bergamof.betgamof.application.model.BankrollView
import fr.bergamof.betgamof.application.model.KellyView
import fr.bergamof.betgamof.application.port.inbound.BankrollCommand
import fr.bergamof.betgamof.application.port.inbound.BankrollUseCases
import fr.bergamof.betgamof.application.port.outbound.BankrollRepository
import fr.bergamof.betgamof.application.port.outbound.TransactionRunner
import fr.bergamof.betgamof.domain.Bankroll
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.BankrollSettings
import fr.bergamof.betgamof.domain.KellyAdvisor
import java.time.Clock

class BankrollService(
    private val portfolio: PortfolioLoader,
    private val repository: BankrollRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) : BankrollUseCases {
    override fun list(): List<BankrollView> =
        transactions.inTransaction {
            val snapshot = portfolio.load()
            snapshot.bankrolls.map { it.toView(snapshot) }
        }

    override fun get(id: BankrollId): BankrollView =
        transactions.inTransaction {
            val snapshot = portfolio.load()
            snapshot.requireBankroll(id).toView(snapshot)
        }

    override fun create(command: BankrollCommand): BankrollView =
        transactions.inTransaction {
            val bankroll = repository.add(Bankroll.open(command.toSettings(), clock.instant()))
            get(bankroll.id)
        }

    override fun update(
        id: BankrollId,
        command: BankrollCommand,
    ): BankrollView =
        transactions.inTransaction {
            val bankroll = repository.findById(id) ?: throw bankrollNotFound(id)
            repository.save(bankroll.reconfigure(command.toSettings()))
            get(id)
        }

    override fun delete(id: BankrollId) =
        transactions.inTransaction {
            val snapshot = portfolio.load()
            snapshot.requireBankroll(id)
            snapshot.ensureBankrollDeletable(id)
            repository.remove(id)
        }

    override fun adviseStake(
        id: BankrollId,
        odds: Double,
    ): KellyView =
        transactions.inTransaction {
            val snapshot = portfolio.load()
            val bankroll = snapshot.requireBankroll(id)
            val advice = KellyAdvisor.advise(odds, snapshot.position(id).balance, bankroll.settings.kellyFraction, snapshot.bets)
            KellyView(advice.stake, advice.bankrollShare, advice.probability)
        }

    private fun BankrollCommand.toSettings() =
        BankrollSettings.of(
            name = name,
            color = color,
            initialBalance = initialBalance,
            stopLoss = stopLoss,
            kellyFraction = kellyFraction ?: BankrollSettings.DEFAULT_KELLY_FRACTION,
            fixedStake = fixedStake,
        )
}
