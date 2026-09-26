package fr.bergamof.betgamof.adapter.demo

import fr.bergamof.betgamof.application.fake.InMemoryPersistence
import fr.bergamof.betgamof.application.port.inbound.BetFilter
import fr.bergamof.betgamof.application.port.inbound.StatsPeriod
import fr.bergamof.betgamof.application.service.BankrollService
import fr.bergamof.betgamof.application.service.BetService
import fr.bergamof.betgamof.application.service.InsightService
import fr.bergamof.betgamof.application.service.MontanteService
import fr.bergamof.betgamof.application.service.PortfolioLoader
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.MontanteStatus
import fr.bergamof.betgamof.domain.RuleKind
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DemoSeederTest {
    private val now = Instant.parse("2026-09-11T16:00:00Z")
    private val zone = ZoneId.of("Europe/Paris")
    private val clock = SettableClock(now)
    private val store = InMemoryPersistence()
    private val portfolio = PortfolioLoader(store.bankrolls, store.bets, store.montantes)
    private val bankrolls = BankrollService(portfolio, store.bankrolls, store.transactions, clock)
    private val bets = BetService(portfolio, store.bets, store.transactions, clock)
    private val montantes = MontanteService(portfolio, store.montantes, store.transactions, clock)
    private val insights = InsightService(portfolio, store.rules, store.transactions, clock, zone)
    private val seeder = DemoSeeder(bankrolls, bets, montantes, insights, clock, zone)

    @Test
    fun `seeds the bankrolls and rules of the mock-ups`() {
        seeder.seedIfEmpty()

        assertEquals(listOf("Principale", "Fun / longs shots"), bankrolls.list().map { it.name })
        assertEquals(
            listOf(RuleKind.MAX_STAKE_PCT to 3, RuleKind.PAUSE_AFTER_LOSSES to 2, RuleKind.SINGLE_ACTIVE_MONTANTE to 0),
            insights.rules().map { it.kind to it.param },
        )
        assertTrue(insights.stats(StatsPeriod.ALL, null).settledCount > 40)
    }

    @Test
    fun `dates the history in the past and puts the clock back`() {
        seeder.seedIfEmpty()

        assertEquals(now, clock.instant())
        val all = bets.list(BetFilter())
        assertTrue(all.minOf { it.placedAt } < now.minusSeconds(90L * 24 * 3_600))
        assertTrue(all.all { it.placedAt < it.startsAt })
        assertTrue(all.filter { it.status != BetStatus.OPEN }.all { requireNotNull(it.settledAt) > it.placedAt })
    }

    @Test
    fun `seeds one active montante and finished ones`() {
        seeder.seedIfEmpty()

        val all = montantes.list()
        assertEquals(
            mapOf(
                "Montante Ligue 1" to MontanteStatus.ACTIVE,
                "Montante Liga" to MontanteStatus.BROKEN,
                "Montante Tennis" to MontanteStatus.SUCCEEDED,
            ),
            all.associate { it.name to it.status },
        )
        val active = all.single { it.status == MontanteStatus.ACTIVE }
        assertEquals(1, active.relancesUsed)
        val openPalier = assertNotNull(active.nextStep?.openBet)
        assertEquals(active.capital, openPalier.stake)
    }

    @Test
    fun `every palier stakes the montante's capital`() {
        seeder.seedIfEmpty()

        val placed = bets.list(BetFilter()).associateBy { it.id }
        val paliers = montantes.list().flatMap { it.paliers }
        assertEquals(9, paliers.size)
        paliers.forEach { palier -> assertEquals(palier.stake, placed.getValue(palier.betId).stake) }
    }

    @Test
    fun `a second run changes nothing`() {
        seeder.seedIfEmpty()
        val betCount = bets.list(BetFilter()).size

        DemoSeeder(bankrolls, bets, montantes, insights, clock, zone).seedIfEmpty()

        assertEquals(2, bankrolls.list().size)
        assertEquals(betCount, bets.list(BetFilter()).size)
        assertEquals(3, montantes.list().size)
        assertEquals(3, insights.rules().size)
    }
}
