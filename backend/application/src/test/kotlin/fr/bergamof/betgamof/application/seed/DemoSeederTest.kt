package fr.bergamof.betgamof.application.seed

import fr.bergamof.betgamof.business.InMemoryBankrollRepository
import fr.bergamof.betgamof.business.InMemoryBetRepository
import fr.bergamof.betgamof.business.InMemoryMontanteRepository
import fr.bergamof.betgamof.business.InMemoryRuleRepository
import fr.bergamof.betgamof.business.domain.BankrollColor
import fr.bergamof.betgamof.business.domain.BankrollSettings
import fr.bergamof.betgamof.business.domain.Money
import fr.bergamof.betgamof.business.domain.MontanteEngine
import fr.bergamof.betgamof.business.domain.MontanteStatus
import fr.bergamof.betgamof.business.port.outbound.BetCriteria
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoSeederTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-11T16:00:00Z"), ZoneOffset.UTC)
    private val bankrolls = InMemoryBankrollRepository()
    private val bets = InMemoryBetRepository()
    private val montantes = InMemoryMontanteRepository()
    private val rules = InMemoryRuleRepository()
    private val seeder = DemoSeeder(bankrolls, bets, montantes, rules, clock, ZoneId.of("Europe/Paris"))

    @Test
    fun `an empty database gets bankrolls, history, montantes and rules`() {
        seeder.seedIfEmpty()

        assertEquals(2, bankrolls.findAll().size)
        assertTrue(bets.find(BetCriteria()).count { it.isSettled } > 40)
        assertEquals(3, rules.findAll().size)
    }

    @Test
    fun `exactly one seeded montante is still active`() {
        seeder.seedIfEmpty()

        val states =
            montantes.findAll().map { montante ->
                MontanteEngine.replay(
                    montante,
                    bets.find(BetCriteria()).filter {
                        it.montanteId ==
                            montante.id
                    },
                )
            }

        assertEquals(1, states.count { it.status == MontanteStatus.ACTIVE })
    }

    @Test
    fun `seeded bets are never placed in the future`() {
        seeder.seedIfEmpty()

        assertTrue(bets.find(BetCriteria()).all { it.placedAt <= clock.instant() })
    }

    @Test
    fun `a database with bankrolls is left untouched`() {
        bankrolls.create(BankrollSettings("Mine", BankrollColor.GAZON, Money.euros(10), null, 0.25, null), clock.instant())

        seeder.seedIfEmpty()

        assertEquals(1, bankrolls.findAll().size)
        assertTrue(bets.find(BetCriteria()).isEmpty())
    }
}
