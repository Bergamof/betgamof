package fr.bergamof.betgamof.application.fake

import fr.bergamof.betgamof.application.contract.PersistencePorts
import fr.bergamof.betgamof.application.port.outbound.BankrollRepository
import fr.bergamof.betgamof.application.port.outbound.BetRepository
import fr.bergamof.betgamof.application.port.outbound.MontanteRepository
import fr.bergamof.betgamof.application.port.outbound.RuleRepository
import fr.bergamof.betgamof.application.port.outbound.TransactionRunner
import fr.bergamof.betgamof.domain.Bankroll
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.Bet
import fr.bergamof.betgamof.domain.BetId
import fr.bergamof.betgamof.domain.DisciplineRule
import fr.bergamof.betgamof.domain.Montante
import fr.bergamof.betgamof.domain.MontanteId
import fr.bergamof.betgamof.domain.RuleId
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// In-memory adapters for the driven ports: use cases are tested without a database.
// They pass the same contract suites as the SQLite adapter (see the `contract` package).

/** A store whose content can be captured and put back, so a failed transaction leaves no trace. */
fun interface Rollbackable {
    /** Captures the current content and returns the action that restores it. */
    fun checkpoint(): () -> Unit
}

/**
 * Stores aggregates by id; ids come from a counter so a removed id is never handed out again.
 * Every operation is atomic on its own, like a single statement on a database.
 */
private class Store<K, V>(
    private val newKey: (Long) -> K,
) : Rollbackable {
    private val rows = linkedMapOf<K, V>()
    private var lastId = 0L

    @Synchronized
    fun all() = rows.values.toList()

    @Synchronized
    operator fun get(key: K) = rows[key]

    @Synchronized
    fun add(build: (K) -> V): V {
        val key = newKey(++lastId)
        return build(key).also { rows[key] = it }
    }

    @Synchronized
    fun replace(
        key: K,
        value: V,
    ) {
        check(key in rows) { "Aggregate $key does not exist" }
        rows[key] = value
    }

    @Synchronized
    fun remove(key: K) {
        rows.remove(key)
    }

    // A rolled-back insert gives its id back, as SQLite does: nothing can refer to it.
    @Synchronized
    override fun checkpoint(): () -> Unit {
        val savedRows = LinkedHashMap(rows)
        val savedLastId = lastId
        return {
            synchronized(this) {
                rows.clear()
                rows.putAll(savedRows)
                lastId = savedLastId
            }
        }
    }
}

class InMemoryBankrollRepository :
    BankrollRepository,
    Rollbackable {
    private val store = Store<BankrollId, Bankroll>(::BankrollId)

    override fun findAll() = store.all()

    override fun findById(id: BankrollId) = store[id]

    override fun add(bankroll: Bankroll) = store.add { bankroll.copy(id = it) }

    override fun save(bankroll: Bankroll) = store.replace(bankroll.id, bankroll)

    override fun remove(id: BankrollId) = store.remove(id)

    override fun checkpoint() = store.checkpoint()
}

class InMemoryBetRepository :
    BetRepository,
    Rollbackable {
    private val store = Store<BetId, Bet>(::BetId)

    override fun findAll() = store.all()

    override fun findById(id: BetId) = store[id]

    override fun add(bet: Bet) = store.add { bet.copy(id = it) }

    override fun save(bet: Bet) = store.replace(bet.id, bet)

    override fun remove(id: BetId) = store.remove(id)

    override fun checkpoint() = store.checkpoint()
}

class InMemoryMontanteRepository :
    MontanteRepository,
    Rollbackable {
    private val store = Store<MontanteId, Montante>(::MontanteId)

    override fun findAll() = store.all()

    override fun findById(id: MontanteId) = store[id]

    override fun add(montante: Montante) = store.add { montante.copy(id = it) }

    override fun save(montante: Montante) = store.replace(montante.id, montante)

    override fun checkpoint() = store.checkpoint()
}

class InMemoryRuleRepository :
    RuleRepository,
    Rollbackable {
    private val store = Store<RuleId, DisciplineRule>(::RuleId)

    override fun findAll() = store.all()

    override fun findById(id: RuleId) = store[id]

    override fun add(rule: DisciplineRule) = store.add { rule.copy(id = it) }

    override fun remove(id: RuleId) = store.remove(id)

    override fun checkpoint() = store.checkpoint()
}

/**
 * Runs use cases one at a time, which is all the isolation an in-memory store needs.
 * The outermost block checkpoints [stores] and restores them if it fails; nested blocks join it.
 */
class InMemoryTransactionRunner(
    private vararg val stores: Rollbackable,
) : TransactionRunner {
    private val lock = ReentrantLock()

    override fun <T> inTransaction(block: () -> T): T =
        lock.withLock {
            if (lock.holdCount > 1) return@withLock block()
            val restores = stores.map { it.checkpoint() }
            try {
                block()
            } catch (
                // Any failure aborts the transaction, whatever its type; it then propagates unchanged.
                @Suppress("TooGenericExceptionCaught") failure: Throwable,
            ) {
                restores.forEach { it() }
                throw failure
            }
        }
}

/** The whole set of fakes, wired like the SQLite persistence façade. */
class InMemoryPersistence : PersistencePorts {
    override val bankrolls = InMemoryBankrollRepository()
    override val bets = InMemoryBetRepository()
    override val montantes = InMemoryMontanteRepository()
    override val rules = InMemoryRuleRepository()
    override val transactions = InMemoryTransactionRunner(bankrolls, bets, montantes, rules)
}
