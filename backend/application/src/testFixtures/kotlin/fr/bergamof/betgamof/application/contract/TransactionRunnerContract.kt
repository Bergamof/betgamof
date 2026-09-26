package fr.bergamof.betgamof.application.contract

import fr.bergamof.betgamof.domain.DisciplineRule
import fr.bergamof.betgamof.domain.RuleKind
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** What every [fr.bergamof.betgamof.application.port.outbound.TransactionRunner] must guarantee. */
abstract class TransactionRunnerContract : PersistenceContract() {
    private val transactions get() = ports.transactions

    private class Boom : RuntimeException("boom")

    @Test
    fun `a block returns its value and commits its writes`() {
        val bankroll = transactions.inTransaction { addBankroll() }

        assertEquals(listOf(bankroll), ports.bankrolls.findAll())
    }

    @Test
    fun `an exception rolls back every write of the block and propagates`() {
        val kept = addBankroll("Avant")
        val failure = Boom()

        val thrown =
            assertFailsWith<Boom> {
                transactions.inTransaction {
                    val bankroll = addBankroll("Pendant")
                    val montante = ports.montantes.add(newMontante(bankroll.id))
                    ports.bets.add(newBet(bankroll.id, montante.id))
                    ports.bankrolls.save(kept.reconfigure(settings(name = "Modifiée")))
                    ports.rules.add(DisciplineRule.define(RuleKind.MAX_STAKE_PCT, param = 5))
                    throw failure
                }
            }

        assertSame(failure, thrown)
        assertEquals(listOf(kept), ports.bankrolls.findAll())
        assertEquals(emptyList(), ports.montantes.findAll())
        assertEquals(emptyList(), ports.bets.findAll())
        assertEquals(emptyList(), ports.rules.findAll())
    }

    @Test
    fun `nested blocks join the outer transaction`() {
        val (outer, inner) =
            transactions.inTransaction {
                val outer = addBankroll("Externe")
                outer to transactions.inTransaction { addBankroll("Interne") }
            }

        assertEquals(listOf(outer, inner), ports.bankrolls.findAll())
    }

    @Test
    fun `an exception escaping a nested block rolls back the outer writes too`() {
        assertFailsWith<Boom> {
            transactions.inTransaction {
                addBankroll("Externe")
                transactions.inTransaction {
                    addBankroll("Interne")
                    throw Boom()
                }
            }
        }

        assertEquals(emptyList(), ports.bankrolls.findAll())
    }

    @Test
    fun `the store is usable again after a rollback`() {
        assertFailsWith<Boom> { transactions.inTransaction { addBankroll().also { throw Boom() } } }

        val bankroll = addBankroll()

        assertEquals(listOf(bankroll), ports.bankrolls.findAll())
    }

    @Test
    fun `concurrent check-then-write blocks are serialised`() {
        val rule = DisciplineRule.define(RuleKind.SINGLE_ACTIVE_MONTANTE, param = 0)

        val added =
            runConcurrently {
                transactions.inTransaction {
                    val absent = ports.rules.findAll().none { it.kind == rule.kind }
                    // Widen the window between the check and the write: without isolation, several threads get through.
                    Thread.sleep(RACE_WINDOW_MS)
                    if (absent) ports.rules.add(rule) else null
                }
            }

        assertEquals(1, added.count { it != null })
        assertEquals(1, ports.rules.findAll().size)
    }

    @Test
    fun `concurrent repository calls outside a transaction each get a distinct id`() {
        val added = runConcurrently { ports.rules.add(DisciplineRule.define(RuleKind.MAX_STAKE_PCT, param = 2)) }

        assertEquals(THREADS, added.map { it.id }.toSet().size)
        assertEquals(
            added.map { it.id }.toSet(),
            ports.rules
                .findAll()
                .map { it.id }
                .toSet(),
        )
    }

    /** Runs [task] on [THREADS] threads released at the same time, and returns what each one produced. */
    private fun <T> runConcurrently(task: () -> T): List<T> {
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(THREADS)
        try {
            val futures =
                List(THREADS) {
                    executor.submit<T> {
                        start.await()
                        task()
                    }
                }
            start.countDown()
            return futures.map { it.get(TIMEOUT_S, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(TIMEOUT_S, TimeUnit.SECONDS))
        }
    }

    private companion object {
        const val THREADS = 8
        const val RACE_WINDOW_MS = 20L
        const val TIMEOUT_S = 30L
    }
}
