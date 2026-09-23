package fr.bergamof.betgamof.api

import fr.bergamof.betgamof.config.AppConfig
import fr.bergamof.betgamof.module
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiTest {
    @TempDir
    lateinit var dir: Path

    private val token = "test-token"
    private val clock = Clock.fixed(Instant.parse("2026-09-11T16:00:00Z"), ZoneOffset.UTC)

    private fun withApi(
        seed: Boolean = false,
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        val config = AppConfig(0, dir.resolve("test.db").toString(), token, ZoneId.of("Europe/Paris"), seed)
        application { module(config, clock) }
        block()
    }

    private suspend fun HttpClient.send(
        path: String,
        body: String? = null,
    ): HttpResponse =
        if (body == null) {
            get(path) { bearerAuth(token) }
        } else {
            post(path) {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }

    private suspend fun HttpResponse.json(): JsonObject = Json.parseToJsonElement(bodyAsText()).jsonObject

    private suspend fun HttpResponse.jsonList(): JsonArray = Json.parseToJsonElement(bodyAsText()).jsonArray

    private fun JsonObject.number(key: String) = getValue(key).jsonPrimitive.double

    private suspend fun HttpClient.createBankroll(): Long =
        send("/api/bankrolls", """{"name":"Principale","color":"GAZON","initialBalance":1000,"stopLoss":900}""")
            .json()
            .getValue("id")
            .jsonPrimitive.long

    private fun simpleBet(
        bankrollId: Long,
        stake: Int,
        odds: Double,
        montanteId: Long? = null,
    ) = """
        {"bankrollId":$bankrollId,"montanteId":$montanteId,"type":"SIMPLE","bookmaker":"Winamax","stake":$stake,"odds":$odds,
         "startsAt":"2026-09-11T19:00:00Z",
         "selections":[{"eventName":"Lens – Lyon","sport":"Football","competition":"Ligue 1","market":"Total de buts","pick":"Plus de 1,5","odds":$odds}]}
        """.trimIndent()

    @Test
    fun `requests without the token are rejected`() =
        withApi {
            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/bankrolls").status)
            assertEquals(HttpStatusCode.OK, client.get("/health").status)
        }

    @Test
    fun `settling a bet updates the bankroll balance`() =
        withApi {
            val bankrollId = client.createBankroll()
            val bet = client.send("/api/bets", simpleBet(bankrollId, 25, 1.72)).json()
            assertEquals("Lens – Lyon · Plus de 1,5", bet.getValue("label").jsonPrimitive.content)

            val settled = client.send("/api/bets/${bet.getValue("id").jsonPrimitive.long}/settlement", """{"status":"WON"}""")
            assertEquals(HttpStatusCode.OK, settled.status)

            val bankroll = client.send("/api/bankrolls/$bankrollId").json()
            assertEquals(1018.0, bankroll.number("balance"))
            assertEquals(118.0, bankroll.number("stopLossMargin"))
        }

    @Test
    fun `settling twice is a conflict`() =
        withApi {
            val bankrollId = client.createBankroll()
            val betId =
                client
                    .send("/api/bets", simpleBet(bankrollId, 10, 2.0))
                    .json()
                    .getValue("id")
                    .jsonPrimitive.long
            client.send("/api/bets/$betId/settlement", """{"status":"LOST"}""")

            assertEquals(HttpStatusCode.Conflict, client.send("/api/bets/$betId/settlement", """{"status":"WON"}""").status)
        }

    @Test
    fun `invalid input is a bad request with a readable message`() =
        withApi {
            val bankrollId = client.createBankroll()
            val response = client.send("/api/bets", simpleBet(bankrollId, 0, 1.5))

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(
                "La mise doit être positive",
                response
                    .json()
                    .getValue("message")
                    .jsonPrimitive.content,
            )
        }

    @Test
    fun `a montante bet stakes the whole capital and drives the montante`() =
        withApi {
            val bankrollId = client.createBankroll()
            val montante =
                client
                    .send(
                        "/api/montantes",
                        """{"name":"Montante Ligue 1","bankrollId":$bankrollId,"startCapital":160,"targetOdds":1.75,
                        "mode":"OBJECTIVE","targetMultiplier":3,"excludeStake":true,"securePct":30}""",
                    ).json()
            val montanteId = montante.getValue("id").jsonPrimitive.long
            assertEquals(840.0, client.send("/api/bankrolls/$bankrollId").json().number("balance"))

            val bet = client.send("/api/bets", simpleBet(bankrollId, 5, 1.75, montanteId)).json()
            assertEquals(160.0, bet.number("stake"))
            client.send("/api/bets/${bet.getValue("id").jsonPrimitive.long}/settlement", """{"status":"WON"}""")

            val after = client.send("/api/montantes/$montanteId").json()
            assertEquals(244.0, after.number("capital"))
            assertEquals(36.0, after.number("secured"))
            assertEquals(
                2,
                after
                    .getValue("currentPalier")
                    .jsonPrimitive.long
                    .toInt(),
            )
        }

    @Test
    fun `montante preview shows the plan and the bankroll after launch`() =
        withApi {
            val bankrollId = client.createBankroll()
            val preview =
                client
                    .send(
                        "/api/montantes/preview",
                        """{"name":"Test","bankrollId":$bankrollId,"startCapital":160,"targetOdds":1.75,
                        "mode":"OBJECTIVE","targetMultiplier":3,"excludeStake":true,"securePct":30}""",
                    ).json()

            assertEquals(480.0, preview.number("target"))
            assertEquals(840.0, preview.number("bankrollBalanceAfterLaunch"))
            assertEquals(3, preview.getValue("steps").jsonArray.size)
        }

    @Test
    fun `demo seed provides a full dashboard`() =
        withApi(seed = true) {
            assertEquals(2, client.send("/api/bankrolls").jsonList().size)
            assertEquals(3, client.send("/api/rules").jsonList().size)
            val stats = client.send("/api/stats?period=all").json()
            assertEquals(true, stats.number("settledCount") > 40)
            assertEquals(true, client.send("/api/events").jsonList().isNotEmpty())
        }
}
