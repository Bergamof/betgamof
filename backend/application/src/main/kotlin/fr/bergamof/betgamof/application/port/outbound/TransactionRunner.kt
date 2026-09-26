package fr.bergamof.betgamof.application.port.outbound

/**
 * Driven port: unit of work. Everything [inTransaction] runs is atomic and isolated from concurrent
 * use cases, so a "load, check, write" sequence cannot interleave with another one.
 * Repository calls made inside the block join the transaction.
 */
interface TransactionRunner {
    fun <T> inTransaction(block: () -> T): T
}
