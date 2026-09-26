package fr.bergamof.betgamof.config

import java.time.ZoneId

private const val DEFAULT_PORT = 8080

data class AppConfig(
    val port: Int,
    val databasePath: String,
    val apiToken: String,
    val zone: ZoneId,
    val seedDemoData: Boolean,
) {
    init {
        require(apiToken.isNotBlank()) { "BETGAMOF_API_TOKEN doit être défini" }
    }

    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()) =
            AppConfig(
                port = env["PORT"]?.toInt() ?: DEFAULT_PORT,
                databasePath = env["BETGAMOF_DB_PATH"] ?: "data/betgamof.db",
                apiToken = env["BETGAMOF_API_TOKEN"].orEmpty(),
                zone = ZoneId.of(env["BETGAMOF_TIMEZONE"] ?: "Europe/Paris"),
                seedDemoData = env["BETGAMOF_SEED_DEMO"].toBoolean(),
            )
    }
}
