package fr.bergamof.betgamof.inbound.rest

import fr.bergamof.betgamof.business.domain.BankrollColor
import fr.bergamof.betgamof.business.domain.BankrollSettings
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class BankrollRoutesTest : RestTest() {
    private val body = """{"name":"Fun","color":"CIEL","initialBalance":250.5,"stopLoss":200,"fixedStake":5}"""
    private val settings = BankrollSettings("Fun", BankrollColor.CIEL, euros(250.5), euros(200.0), 0.25, euros(5.0))

    @Test
    fun `bankrolls are listed with amounts in euros`() =
        api {
            every { bankrolls.list() } returns listOf(bankrollView)

            val bankroll = client.call(HttpMethod.Get, "/api/bankrolls").firstOfList()

            assertEquals("1018.5", bankroll.at("balance"))
            assertEquals("GAZON", bankroll.at("color"))
            assertEquals("null", bankroll.at("fixedStake"))
        }

    @Test
    fun `a bankroll is created from its settings with a quarter Kelly by default`() =
        api {
            every { bankrolls.create(settings) } returns bankrollView

            val response = client.call(HttpMethod.Post, "/api/bankrolls", body)

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("1", response.jsonObject().at("id"))
        }

    @Test
    fun `a bankroll is read by id`() =
        api {
            every { bankrolls.get(1) } returns bankrollView

            assertEquals("118.5", client.call(HttpMethod.Get, "/api/bankrolls/1").jsonObject().at("stopLossMargin"))
        }

    @Test
    fun `a bankroll is updated with new settings`() =
        api {
            every { bankrolls.update(1, settings) } returns bankrollView

            assertEquals(HttpStatusCode.OK, client.call(HttpMethod.Put, "/api/bankrolls/1", body).status)
        }

    @Test
    fun `a deleted bankroll answers no content`() =
        api {
            justRun { bankrolls.delete(1) }

            assertEquals(HttpStatusCode.NoContent, client.call(HttpMethod.Delete, "/api/bankrolls/1").status)
            verify { bankrolls.delete(1) }
        }

    @Test
    fun `kelly advice is computed for the requested odds`() =
        api {
            every { bets.kelly(1, 1.8) } returns kellyView

            val advice = client.call(HttpMethod.Get, "/api/bankrolls/1/kelly?odds=1.8").jsonObject()

            assertEquals("12.0", advice.at("stake"))
            assertEquals("0.6", advice.at("probability"))
        }

    @Test
    fun `kelly advice needs numeric odds`() =
        api {
            val response = client.call(HttpMethod.Get, "/api/bankrolls/1/kelly?odds=abc")

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("Paramètre odds invalide", response.jsonObject().at("message"))
        }
}
