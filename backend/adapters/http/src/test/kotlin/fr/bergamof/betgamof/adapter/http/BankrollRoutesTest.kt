package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.port.inbound.BankrollCommand
import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.Money
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class BankrollRoutesTest {
    private val bankrollJson =
        """
        {"id":7,"name":"Principale","color":"CITRON","initialBalance":1000.0,"balance":1018.5,"stopLoss":900.0,
         "stopLossMargin":118.5,"kellyFraction":0.25,"fixedStake":null,"outsideMontantes":1018.5,"openStake":0.0,
         "staked":25.0,"profit":18.5,"roi":0.74,"betCount":1,"bookmakers":["Winamax"]}
        """

    @Test
    fun `lists bankrolls`() =
        withApi {
            val response = client.call(HttpMethod.Get, "/api/bankrolls")
            assertEquals(HttpStatusCode.OK, response.status)
            assertJson("[$bankrollJson]", response.json())
        }

    @Test
    fun `creates a bankroll, leaving the Kelly default to the domain`() {
        val calls = Calls()
        withApi(calls) {
            val created = client.call(HttpMethod.Post, "/api/bankrolls", """{"name":"Principale","color":"CIEL","initialBalance":1000.5}""")
            assertEquals(HttpStatusCode.Created, created.status)
            assertJson(bankrollJson, created.json())
        }
        assertEquals(
            BankrollCommand("Principale", BankrollColor.CIEL, Money(100_050), stopLoss = null, kellyFraction = null, fixedStake = null),
            calls.last(),
        )
    }

    @Test
    fun `updates a bankroll with every field`() {
        val calls = Calls()
        withApi(calls) {
            val body =
                """{"name":"B","color":"BRIQUE","initialBalance":500,"stopLoss":400,"kellyFraction":0.5,"fixedStake":12.34}"""
            assertEquals(HttpStatusCode.OK, client.call(HttpMethod.Put, "/api/bankrolls/7", body).status)
        }
        assertEquals(BankrollId(7), calls.received[0])
        assertEquals(
            BankrollCommand("B", BankrollColor.BRIQUE, Money.euros(500), Money.euros(400), 0.5, Money(1234)),
            calls.received[1],
        )
    }

    @Test
    fun `gets and deletes a bankroll`() {
        val calls = Calls()
        withApi(calls) {
            assertJson(bankrollJson, client.call(HttpMethod.Get, "/api/bankrolls/7").json())
            val deleted = client.call(HttpMethod.Delete, "/api/bankrolls/8")
            assertEquals(HttpStatusCode.NoContent, deleted.status)
            assertEquals("", deleted.bodyAsText())
        }
        assertEquals(listOf<Any?>(BankrollId(7), BankrollId(8)), calls.received)
    }

    @Test
    fun `advises a Kelly stake`() {
        val calls = Calls()
        withApi(calls) {
            val response = client.call(HttpMethod.Get, "/api/bankrolls/7/kelly?odds=2.5")
            assertEquals(HttpStatusCode.OK, response.status)
            assertJson("""{"stake":12.0,"bankrollShare":0.012,"probability":0.6}""", response.json())
        }
        assertEquals(listOf<Any?>(BankrollId(7), 2.5), calls.received)
    }
}
