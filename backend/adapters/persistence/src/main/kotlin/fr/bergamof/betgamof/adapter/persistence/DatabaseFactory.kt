package fr.bergamof.betgamof.adapter.persistence

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.io.File

private const val BUSY_TIMEOUT_MS = 5_000

internal object DatabaseFactory {
    /** Opens (and migrates) the SQLite file at [path]. */
    fun connect(path: String): Database {
        File(path).absoluteFile.parentFile?.mkdirs()
        val dataSource =
            SQLiteDataSource(
                SQLiteConfig().apply {
                    enforceForeignKeys(true)
                    setJournalMode(SQLiteConfig.JournalMode.WAL)
                    // Take the write lock when a transaction starts, so a read-then-write never fails half-way
                    // because another connection wrote in between; wait for that lock instead of failing at once.
                    setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE)
                    setBusyTimeout(BUSY_TIMEOUT_MS)
                },
            ).apply { url = "jdbc:sqlite:$path" }
        Flyway
            .configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        // A transaction runs a whole use case: never replay it behind the application's back.
        return Database.connect(dataSource, databaseConfig = DatabaseConfig { defaultMaxAttempts = 1 })
    }
}
