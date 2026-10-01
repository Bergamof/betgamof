package fr.bergamof.betgamof.business.service

import fr.bergamof.betgamof.business.InMemoryBankrollRepository
import fr.bergamof.betgamof.business.InMemoryBetRepository
import fr.bergamof.betgamof.business.InMemoryMontanteRepository
import fr.bergamof.betgamof.business.InMemoryRuleRepository
import fr.bergamof.betgamof.business.domain.BankrollColor
import fr.bergamof.betgamof.business.domain.BankrollSettings
import fr.bergamof.betgamof.business.domain.BetStatus
import fr.bergamof.betgamof.business.domain.BetType
import fr.bergamof.betgamof.business.domain.Money
import fr.bergamof.betgamof.business.domain.MontanteConfig
import fr.bergamof.betgamof.business.domain.MontanteMode
import fr.bergamof.betgamof.business.domain.NewBet
import fr.bergamof.betgamof.business.domain.Selection
import fr.bergamof.betgamof.business.domain.Settlement
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Every service wired on in-memory repositories, with helpers to build a betting history. */
class ServiceFixture {
    val clock: Clock = Clock.fixed(Instant.parse("2026-09-11T16:00:00Z"), ZoneOffset.UTC)
    val bankrollRepository = InMemoryBankrollRepository()
    val betRepository = InMemoryBetRepository()
    val montanteRepository = InMemoryMontanteRepository()
    val ruleRepository = InMemoryRuleRepository()
    private val portfolio = Portfolio(bankrollRepository, betRepository, montanteRepository)
    val bankrolls = BankrollService(portfolio, bankrollRepository, betRepository, montanteRepository, clock)
    val bets = BetService(portfolio, betRepository, montanteRepository, clock)
    val montantes = MontanteService(portfolio, montanteRepository, clock)
    val insights = InsightService(portfolio, betRepository, montanteRepository, ruleRepository, clock, ZoneId.of("Europe/Paris"))

    fun bankroll(
        initialBalance: Int = 1000,
        stopLoss: Money? = null,
    ) = bankrolls.create(BankrollSettings("Principale", BankrollColor.GAZON, Money.euros(initialBalance), stopLoss, 0.25, null)).id

    fun newBet(
        bankrollId: Long,
        stake: Int = 10,
        odds: Double = 2.0,
        montanteId: Long? = null,
        bookmaker: String = "Winamax",
        sport: String = "Football",
        startsInSeconds: Long = 3_600,
    ) = NewBet(
        bankrollId = bankrollId,
        montanteId = montanteId,
        type = BetType.SIMPLE,
        bookmaker = bookmaker,
        stake = Money.euros(stake),
        odds = odds,
        startsAt = clock.instant().plusSeconds(startsInSeconds),
        selections = listOf(Selection(null, "Lens – Lyon", sport, "Ligue 1", "Total de buts", "Plus de 1,5", odds)),
    )

    fun placeAndSettle(
        bet: NewBet,
        status: BetStatus,
    ) = bets.settle(bets.create(bet).id, Settlement(status, null))

    fun montanteConfig(
        bankrollId: Long,
        excludeStake: Boolean = true,
        relancesAllowed: Int = 0,
        mode: MontanteMode = MontanteMode.OBJECTIVE,
    ) = MontanteConfig(
        name = "Montante",
        bankrollId = bankrollId,
        startCapital = Money.euros(160),
        targetOdds = 1.75,
        mode = mode,
        targetMultiplier = if (mode == MontanteMode.OBJECTIVE) 3.0 else null,
        stepCount = if (mode == MontanteMode.STEPS) 2 else null,
        excludeStake = excludeStake,
        securePct = 30,
        relancesAllowed = relancesAllowed,
    )
}
