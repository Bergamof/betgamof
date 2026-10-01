package fr.bergamof.betgamof.inbound.rest

import fr.bergamof.betgamof.business.domain.MontanteConfig
import fr.bergamof.betgamof.business.domain.MontanteMode
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.mockk.every
import kotlin.test.Test
import kotlin.test.assertEquals

class MontanteRoutesTest : RestTest() {
    private val body =
        """{"name":"Montante Ligue 1","bankrollId":1,"startCapital":160,"targetOdds":1.75,
           "mode":"OBJECTIVE","targetMultiplier":3,"excludeStake":true,"securePct":30}"""
    private val config = MontanteConfig("Montante Ligue 1", 1, euros(160.0), 1.75, MontanteMode.OBJECTIVE, 3.0, null, true, 30, 0)

    @Test
    fun `montantes are listed with their paliers and next step`() =
        api {
            every { montantes.list() } returns listOf(montanteView)

            val montante = client.call(HttpMethod.Get, "/api/montantes").firstOfList()

            assertEquals("244.0", montante.at("capital"))
            assertEquals("36.0", montante.at("paliers.0.secured"))
            assertEquals("true", montante.at("nextStep.ifLost.relance"))
            assertEquals("Winamax", montante.at("nextStep.openBet.bookmaker"))
        }

    @Test
    fun `a montante is created from its configuration`() =
        api {
            every { montantes.create(config) } returns montanteView

            val response = client.call(HttpMethod.Post, "/api/montantes", body)

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("OBJECTIVE", response.jsonObject().at("mode"))
        }

    @Test
    fun `a montante preview returns the plan`() =
        api {
            every { montantes.preview(config) } returns planView

            val plan = client.call(HttpMethod.Post, "/api/montantes/preview", body).jsonObject()

            assertEquals("840.0", plan.at("bankrollBalanceAfterLaunch"))
            assertEquals("244.0", plan.at("steps.0.capitalAfter"))
        }

    @Test
    fun `a montante is read by id`() =
        api {
            every { montantes.get(3) } returns montanteView

            assertEquals("Montante Ligue 1", client.call(HttpMethod.Get, "/api/montantes/3").jsonObject().at("name"))
        }

    @Test
    fun `a montante is closed`() =
        api {
            every { montantes.close(3) } returns montanteView

            assertEquals(HttpStatusCode.OK, client.call(HttpMethod.Post, "/api/montantes/3/close").status)
        }
}
