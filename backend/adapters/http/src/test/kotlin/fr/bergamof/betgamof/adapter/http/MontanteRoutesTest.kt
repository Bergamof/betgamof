package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.port.inbound.StartMontanteCommand
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteId
import fr.bergamof.betgamof.domain.MontanteMode
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class MontanteRoutesTest {
    private val request =
        """
        {"name":"Montante été","bankrollId":7,"startCapital":160,"targetOdds":1.5,"mode":"STEPS","stepCount":3,
         "excludeStake":true,"securePct":15}
        """

    private val command =
        StartMontanteCommand(
            name = "Montante été",
            bankrollId = BankrollId(7),
            startCapital = Money.euros(160),
            targetOdds = 1.5,
            mode = MontanteMode.STEPS,
            targetMultiplier = null,
            stepCount = 3,
            excludeStake = true,
            securePct = 15,
            relancesAllowed = 0,
        )

    @Test
    fun `serializes a montante with its paliers and next step`() =
        withApi {
            val response = client.call(HttpMethod.Get, "/api/montantes/3")
            assertEquals(HttpStatusCode.OK, response.status)
            val montante = response.jsonObject()
            val expectedPalier =
                """
                {"number":1,"betId":44,"label":"Système 2 sélections","bookmaker":"Betclic","startsAt":"2026-09-11T19:00:00Z",
                 "stake":100.0,"odds":2.5,"status":"LOST","secured":10.0,"capitalAfter":0.0,"isRelance":false}
                """
            assertJson("[$expectedPalier]", montante.getValue("paliers"))
            val nextStep = montante.getValue("nextStep").jsonObject
            assertJson(
                """{"relance":false,"capitalAfter":0.0,"securedKept":10.0,"lostAmount":90.0}""",
                nextStep.getValue("ifLost"),
            )
            assertEquals(
                "\"Combiné 3 sélections\"",
                nextStep
                    .getValue("openBet")
                    .jsonObject
                    .getValue("label")
                    .toString(),
            )
            val summary = montante.filterKeys { it != "paliers" && it != "nextStep" }
            assertJson(
                """
                {"id":3,"name":"Montante été","bankrollId":7,"bankrollName":"Principale","mode":"OBJECTIVE","targetMultiplier":3.0,
                 "stepCount":null,"targetOdds":1.5,"excludeStake":true,"securePct":10,"relancesAllowed":1,"relancesUsed":1,
                 "startCapital":100.0,"status":"BROKEN","capital":0.0,"engaged":200.0,"secured":10.0,"result":-190.0,"target":300.0,
                 "plannedSteps":3,"currentPalier":2,"totalPaliers":3,"progress":0.5,"successProbability":null,
                 "createdAt":"2026-09-10T20:00:00Z","closedAt":null}
                """,
                JsonObject(summary),
            )
        }

    @Test
    fun `starts a montante`() {
        val calls = Calls()
        withApi(calls) {
            assertEquals(HttpStatusCode.Created, client.call(HttpMethod.Post, "/api/montantes", request).status)
        }
        assertEquals(command, calls.last())
    }

    @Test
    fun `previews a montante`() {
        val calls = Calls()
        withApi(calls) {
            val response = client.call(HttpMethod.Post, "/api/montantes/preview", request)
            assertEquals(HttpStatusCode.OK, response.status)
            assertJson(
                """
                {"target":480.0,"plannedSteps":1,"steps":[{"number":1,"stake":160.0,"secured":24.0,"capitalAfter":216.0}],
                 "successProbability":0.3,"bankrollBalance":1000.0,"bankrollBalanceAfterLaunch":840.0}
                """,
                response.json(),
            )
        }
        assertEquals(command, calls.last())
    }

    @Test
    fun `lists and closes montantes`() {
        val calls = Calls()
        withApi(calls) {
            assertEquals(HttpStatusCode.OK, client.call(HttpMethod.Get, "/api/montantes").status)
            assertEquals(HttpStatusCode.OK, client.call(HttpMethod.Post, "/api/montantes/3/close").status)
        }
        assertEquals(listOf<Any?>(MontanteId(3)), calls.received)
    }
}
