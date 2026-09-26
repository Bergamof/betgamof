package fr.bergamof.betgamof.application

import fr.bergamof.betgamof.application.fake.InMemoryPersistence
import fr.bergamof.betgamof.application.port.inbound.AmendBetCommand
import fr.bergamof.betgamof.application.port.inbound.BankrollCommand
import fr.bergamof.betgamof.application.port.inbound.PlaceBetCommand
import fr.bergamof.betgamof.application.port.inbound.SelectionCommand
import fr.bergamof.betgamof.application.port.inbound.SettleBetCommand
import fr.bergamof.betgamof.application.port.inbound.StartMontanteCommand
import fr.bergamof.betgamof.application.service.BankrollService
import fr.bergamof.betgamof.application.service.BetService
import fr.bergamof.betgamof.application.service.MontanteService
import fr.bergamof.betgamof.application.service.PortfolioLoader
import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.BankrollSettings
import fr.bergamof.betgamof.domain.BetId
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.InvalidTransitionException
import fr.bergamof.betgamof.domain.InvalidValueException
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteId
import fr.bergamof.betgamof.domain.MontanteMode
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UseCaseTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-11T16:00:00Z"), ZoneOffset.UTC)
    private val store = InMemoryPersistence()
    private val portfolio = PortfolioLoader(store.bankrolls, store.bets, store.montantes)
    private val bankrolls = BankrollService(portfolio, store.bankrolls, store.transactions, clock)
    private val bets = BetService(portfolio, store.bets, store.transactions, clock)
    private val montantes = MontanteService(portfolio, store.montantes, store.transactions, clock)

    private val bankrollId = bankrolls.create(BankrollCommand("Principale", BankrollColor.GAZON, Money.euros(1000))).id

    private fun placeBet(
        stake: Int,
        odds: Double,
        montanteId: MontanteId? = null,
        bankroll: BankrollId = bankrollId,
    ) = PlaceBetCommand(
        bankrollId = bankroll,
        montanteId = montanteId,
        type = BetType.SIMPLE,
        bookmaker = "Winamax",
        stake = Money.euros(stake),
        odds = odds,
        startsAt = clock.instant().plusSeconds(3_600),
        selections = listOf(SelectionCommand(null, "Lens – Lyon", "Football", "Ligue 1", "Total de buts", "Plus de 1,5", odds)),
    )

    private fun startMontante(excludeStake: Boolean = true) =
        StartMontanteCommand("Montante", bankrollId, Money.euros(160), 1.75, MontanteMode.OBJECTIVE, 3.0, null, excludeStake, 30, 0)

    @Test
    fun `a bankroll without Kelly fraction gets the domain default`() {
        assertEquals(BankrollSettings.DEFAULT_KELLY_FRACTION, bankrolls.get(bankrollId).kellyFraction)
    }

    @Test
    fun `a settled bet moves the bankroll balance`() {
        val bet = bets.place(placeBet(stake = 25, odds = 1.72))
        bets.settle(bet.id, SettleBetCommand(BetStatus.WON))

        assertEquals(Money.euros(1018), bankrolls.get(bankrollId).balance)
    }

    @Test
    fun `a bet cannot be settled twice`() {
        val bet = bets.place(placeBet(stake = 10, odds = 2.0))
        bets.settle(bet.id, SettleBetCommand(BetStatus.LOST))

        assertFailsWith<InvalidTransitionException> { bets.settle(bet.id, SettleBetCommand(BetStatus.WON)) }
    }

    @Test
    fun `an invalid request is reported by the domain`() {
        assertFailsWith<InvalidValueException> { bets.place(placeBet(stake = 0, odds = 2.0)) }
        val bet = bets.place(placeBet(stake = 10, odds = 2.0))
        assertFailsWith<InvalidValueException> { bets.amend(bet.id, AmendBetCommand(Money.euros(10), 0.5, "Winamax")) }
    }

    @Test
    fun `a montante bet stakes the whole capital whatever the requested stake`() {
        val montante = montantes.start(startMontante())

        val bet = bets.place(placeBet(stake = 5, odds = 1.75, montanteId = montante.id))

        assertEquals(Money.euros(160), bet.stake)
        assertEquals(Money.euros(840), bankrolls.get(bankrollId).balance)
    }

    @Test
    fun `a palier must be placed on the montante's bankroll`() {
        val other = bankrolls.create(BankrollCommand("Réserve", BankrollColor.CIEL, Money.euros(100))).id
        val montante = montantes.start(startMontante())

        assertFailsWith<InvalidValueException> { bets.place(placeBet(stake = 5, odds = 1.75, montanteId = montante.id, bankroll = other)) }
    }

    @Test
    fun `a montante with a pending palier cannot be closed`() {
        val montante = montantes.start(startMontante())
        bets.place(placeBet(stake = 160, odds = 1.75, montanteId = montante.id))

        assertFailsWith<InvalidTransitionException> { montantes.close(montante.id) }
    }

    @Test
    fun `closing an excluded montante returns its balance to the bankroll`() {
        val montante = montantes.start(startMontante())
        val palier = bets.place(placeBet(stake = 160, odds = 1.75, montanteId = montante.id))
        bets.settle(palier.id, SettleBetCommand(BetStatus.WON))

        montantes.close(montante.id)

        // 1000 - 160 engaged + 36 secured + 244 capital back.
        assertEquals(Money.euros(1120), bankrolls.get(bankrollId).balance)
    }

    @Test
    fun `a bankroll with bets cannot be deleted`() {
        bets.place(placeBet(stake = 10, odds = 2.0))

        assertFailsWith<InvalidTransitionException> { bankrolls.delete(bankrollId) }
    }

    @Test
    fun `unknown ids are reported as not found`() {
        assertFailsWith<NotFoundException> { bets.get(BetId(42)) }
        assertFailsWith<NotFoundException> { bets.settle(BetId(42), SettleBetCommand(BetStatus.WON)) }
        assertFailsWith<NotFoundException> { montantes.get(MontanteId(42)) }
        assertFailsWith<NotFoundException> { bankrolls.update(BankrollId(42), BankrollCommand("X", BankrollColor.CIEL, Money.ZERO)) }
    }
}
