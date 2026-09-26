package fr.bergamof.betgamof.adapter.persistence

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.io.File

internal object DatabaseFactory {
    /** Opens (and migrates) the SQLite file at [path]. */
    fun connect(path: String): Database {
        File(path).absoluteFile.parentFile?.mkdirs()
        val dataSource =
            SQLiteDataSource(
                SQLiteConfig().apply {
                    enforceForeignKeys(true)
                    setJournalMode(SQLiteConfig.JournalMode.WAL)
                },
            ).apply { url = "jdbc:sqlite:$path" }
        Flyway
            .configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        return Database.connect(dataSource)
    }
}
