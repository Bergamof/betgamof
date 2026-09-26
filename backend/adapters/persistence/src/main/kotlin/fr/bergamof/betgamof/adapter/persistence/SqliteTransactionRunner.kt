package fr.bergamof.betgamof.adapter.persistence

import fr.bergamof.betgamof.application.port.outbound.TransactionRunner
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Unit of work over one SQLite database, used by the application and by every repository call.
 *
 * Isolation: SQLite has a single writer, and a deferred transaction that reads then writes fails with
 * `SQLITE_BUSY` (it cannot upgrade its lock) instead of waiting when another connection wrote meanwhile.
 * Retrying would replay the use case; letting it fail would surface random errors under load (Netty
 * serves requests on several threads). So transactions on a database are serialised in the JVM by a
 * [ReentrantLock] shared by every [SqlitePersistence] opened on the same file: a "load, check, write"
 * block runs alone from its first read to its commit, which is serialisable isolation. A betting log
 * has one user and short transactions, so the lost read concurrency does not matter.
 * Connections also start their transactions with `BEGIN IMMEDIATE` and wait on a busy timeout
 * (see [DatabaseFactory]), which covers another process writing to the same file.
 *
 * Joining: the lock is reentrant, and Exposed's `transaction(db)` called while a transaction on the same
 * database is open on the thread runs in it (no nested transactions are configured). A repository call
 * made inside [inTransaction] therefore joins the use case's transaction, and outside it runs in its own.
 * An exception escaping the outermost block rolls everything back.
 */
internal class SqliteTransactionRunner(
    private val db: Database,
    private val lock: ReentrantLock,
) : TransactionRunner {
    override fun <T> inTransaction(block: () -> T): T = execute { block() }

    /** Runs [statement] in the current transaction, or in a new one; for the repositories. */
    fun <T> execute(statement: JdbcTransaction.() -> T): T = lock.withLock { transaction(db) { statement() } }

    companion object {
        private val locks = ConcurrentHashMap<String, ReentrantLock>()

        /** The lock of the database file at [path], shared JVM-wide. */
        fun lockFor(path: String): ReentrantLock = locks.computeIfAbsent(File(path).canonicalPath) { ReentrantLock() }
    }
}
