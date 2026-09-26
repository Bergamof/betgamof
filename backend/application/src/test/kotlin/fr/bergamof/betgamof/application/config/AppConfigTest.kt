package fr.bergamof.betgamof.application.config

import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppConfigTest {
    @Test
    fun `defaults apply when only the token is set`() {
        val config = AppConfig.fromEnvironment(mapOf("BETGAMOF_API_TOKEN" to "secret"))

        assertEquals(AppConfig(8080, "data/betgamof.db", "secret", ZoneId.of("Europe/Paris"), seedDemoData = false), config)
    }

    @Test
    fun `every setting is read from the environment`() {
        val env =
            mapOf(
                "PORT" to "9090",
                "BETGAMOF_DB_PATH" to "/data/b.db",
                "BETGAMOF_API_TOKEN" to "secret",
                "BETGAMOF_TIMEZONE" to "UTC",
                "BETGAMOF_SEED_DEMO" to "true",
            )

        assertEquals(AppConfig(9090, "/data/b.db", "secret", ZoneId.of("UTC"), seedDemoData = true), AppConfig.fromEnvironment(env))
    }

    @Test
    fun `the API token is mandatory`() {
        val error = assertFailsWith<IllegalArgumentException> { AppConfig.fromEnvironment(emptyMap()) }

        assertEquals("BETGAMOF_API_TOKEN doit être défini", error.message)
    }
}
