package fr.bergamof.betgamof.application

import fr.bergamof.betgamof.application.service.BankrollService
import fr.bergamof.betgamof.application.service.BetService
import fr.bergamof.betgamof.application.service.MontanteService
import fr.bergamof.betgamof.application.service.PortfolioLoader
import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BankrollSettings
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteConfig
import fr.bergamof.betgamof.domain.MontanteMode
import fr.bergamof.betgamof.domain.NewBet
import fr.bergamof.betgamof.domain.Selection
import fr.bergamof.betgamof.domain.Settlement
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UseCaseTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-11T16:00:00Z"), ZoneOffset.UTC)
    private val bankrollRepository = InMemoryBankrollRepository()
    private val betRepository = InMemoryBetRepository()
    private val montanteRepository = InMemoryMontanteRepository()
    private val portfolio = PortfolioLoader(bankrollRepository, betRepository, montanteRepository)
    private val bankrolls = BankrollService(portfolio, bankrollRepository, clock)
    private val bets = BetService(portfolio, betRepository, clock)
    private val montantes = MontanteService(portfolio, montanteRepository, clock)

    private val bankrollId =
        bankrolls.create(BankrollSettings("Principale", BankrollColor.GAZON, Money.euros(1000), null, 0.25, null)).id

    private fun newBet(
        stake: Int,
        odds: Double,
        montanteId: Long? = null,
    ) = NewBet(
        bankrollId = bankrollId,
        montanteId = montanteId,
        type = BetType.SIMPLE,
        bookmaker = "Winamax",
        stake = Money.euros(stake),
        odds = odds,
        startsAt = clock.instant().plusSeconds(3_600),
        selections = listOf(Selection(null, "Lens – Lyon", "Football", "Ligue 1", "Total de buts", "Plus de 1,5", odds)),
    )

    private fun montanteConfig(excludeStake: Boolean = true) =
        MontanteConfig("Montante", bankrollId, Money.euros(160), 1.75, MontanteMode.OBJECTIVE, 3.0, null, excludeStake, 30, 0)

    @Test
    fun `a settled bet moves the bankroll balance`() {
        val bet = bets.create(newBet(stake = 25, odds = 1.72))
        bets.settle(bet.id, Settlement(BetStatus.WON, null))

        assertEquals(Money.euros(1018), bankrolls.get(bankrollId).balance)
    }

    @Test
    fun `a bet cannot be settled twice`() {
        val bet = bets.create(newBet(stake = 10, odds = 2.0))
        bets.settle(bet.id, Settlement(BetStatus.LOST, null))

        assertFailsWith<ConflictException> { bets.settle(bet.id, Settlement(BetStatus.WON, null)) }
    }

    @Test
    fun `a montante bet stakes the whole capital whatever the requested stake`() {
        val montante = montantes.create(montanteConfig())

        val bet = bets.create(newBet(stake = 5, odds = 1.75, montanteId = montante.id))

        assertEquals(Money.euros(160), bet.stake)
        assertEquals(Money.euros(840), bankrolls.get(bankrollId).balance)
    }

    @Test
    fun `a montante with a pending palier cannot be closed`() {
        val montante = montantes.create(montanteConfig())
        bets.create(newBet(stake = 160, odds = 1.75, montanteId = montante.id))

        assertFailsWith<ConflictException> { montantes.close(montante.id) }
    }

    @Test
    fun `closing an excluded montante returns its balance to the bankroll`() {
        val montante = montantes.create(montanteConfig())
        val palier = bets.create(newBet(stake = 160, odds = 1.75, montanteId = montante.id))
        bets.settle(palier.id, Settlement(BetStatus.WON, null))

        montantes.close(montante.id)

        // 1000 - 160 engaged + 36 secured + 244 capital back.
        assertEquals(Money.euros(1120), bankrolls.get(bankrollId).balance)
    }

    @Test
    fun `unknown ids are reported as not found`() {
        assertFailsWith<NotFoundException> { bets.get(42) }
        assertFailsWith<NotFoundException> { montantes.get(42) }
    }
}
