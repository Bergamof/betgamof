package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.port.inbound.AmendBetCommand
import fr.bergamof.betgamof.application.port.inbound.BetFilter
import fr.bergamof.betgamof.application.port.inbound.PlaceBetCommand
import fr.bergamof.betgamof.application.port.inbound.SelectionCommand
import fr.bergamof.betgamof.application.port.inbound.SettleBetCommand
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.BetId
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteId
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class BetRoutesTest {
    private val simpleBetJson =
        """
        {"id":42,"bankrollId":7,"bankrollName":"Principale","montanteId":null,"montanteName":null,"palierNumber":null,
         "type":"SIMPLE","bookmaker":"Winamax","stake":25.0,"odds":1.74,"status":"WON","cashout":null,"profit":18.5,
         "potentialReturn":43.5,"label":"Lens – Lyon · Plus de 1,5","sport":"Football","competition":"Ligue 1","market":"Buts",
         "selections":[{"eventId":"evt-1","eventName":"Lens – Lyon","sport":"Football","competition":"Ligue 1","market":"Buts",
                        "pick":"Plus de 1,5","odds":1.74}],
         "placedAt":"2026-09-10T20:00:00Z","startsAt":"2026-09-11T19:00:00Z","settledAt":"2026-09-11T21:00:00Z"}
        """

    @Test
    fun `serializes a simple bet with its presentation fields`() =
        withApi {
            val response = client.call(HttpMethod.Get, "/api/bets/42")
            assertEquals(HttpStatusCode.OK, response.status)
            assertJson(simpleBetJson, response.json())
        }

    @Test
    fun `formats a combined bet`() =
        withApi {
            val combined =
                client
                    .call(HttpMethod.Get, "/api/bets")
                    .json()
                    .jsonArray[1]
                    .jsonObject

            fun field(name: String) = combined.getValue(name).toString()
            assertEquals("\"Combiné 3 sélections\"", field("label"))
            assertEquals("\"Ligue 1 · Roland-Garros\"", field("competition"))
            assertEquals("\"Combiné\"", field("market"))
            assertEquals("\"COMBINE\"", field("type"))
            assertEquals("\"CASHOUT\"", field("status"))
            assertEquals("30.0", field("cashout"))
            assertEquals("3", field("montanteId"))
            assertEquals("null", field("settledAt"))
        }

    @Test
    fun `passes the list filter, parsing the status case-insensitively`() {
        val calls = Calls()
        withApi(calls) {
            val response = client.call(HttpMethod.Get, "/api/bets?status=open&bankrollId=7&sport=%20&bookmaker=Winamax&lastDays=30&q=lens")
            assertEquals(HttpStatusCode.OK, response.status)
        }
        assertEquals(BetFilter(BetStatus.OPEN, BankrollId(7), null, "Winamax", 30, "lens"), calls.last())
    }

    @Test
    fun `places a bet from a command`() {
        val calls = Calls()
        withApi(calls) {
            val body =
                """
                {"bankrollId":7,"montanteId":3,"type":"SIMPLE","bookmaker":"Winamax","stake":25.5,"odds":1.74,
                 "startsAt":"2026-09-11T19:00:00Z","selections":[{"eventName":"Lens – Lyon","sport":"Football","pick":"Lens","odds":1.74}]}
                """
            val response = client.call(HttpMethod.Post, "/api/bets", body)
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(
                42,
                response
                    .jsonObject()
                    .getValue("id")
                    .jsonPrimitive.content
                    .toInt(),
            )
        }
        assertEquals(
            PlaceBetCommand(
                bankrollId = BankrollId(7),
                montanteId = MontanteId(3),
                type = BetType.SIMPLE,
                bookmaker = "Winamax",
                stake = Money(2550),
                odds = 1.74,
                startsAt = Instant.parse("2026-09-11T19:00:00Z"),
                selections = listOf(SelectionCommand(null, "Lens – Lyon", "Football", "", "", "Lens", 1.74)),
            ),
            calls.last(),
        )
    }

    @Test
    fun `amends a bet`() {
        val calls = Calls()
        withApi(calls) {
            val response = client.call(HttpMethod.Put, "/api/bets/42", """{"stake":30,"odds":1.8,"bookmaker":"Unibet"}""")
            assertEquals(HttpStatusCode.OK, response.status)
        }
        assertEquals(listOf<Any?>(BetId(42), AmendBetCommand(Money.euros(30), 1.8, "Unibet")), calls.received)
    }

    @Test
    fun `settles a bet`() {
        val calls = Calls()
        withApi(calls) {
            val response = client.call(HttpMethod.Post, "/api/bets/42/settlement", """{"status":"CASHOUT","cashout":12.5}""")
            assertEquals(HttpStatusCode.OK, response.status)
            client.call(HttpMethod.Post, "/api/bets/43/settlement", """{"status":"VOID"}""")
        }
        assertEquals(
            listOf<Any?>(
                BetId(42),
                SettleBetCommand(BetStatus.CASHOUT, Money(1250)),
                BetId(43),
                SettleBetCommand(BetStatus.VOID, null),
            ),
            calls.received,
        )
    }

    @Test
    fun `deletes a bet`() {
        val calls = Calls()
        withApi(calls) { assertEquals(HttpStatusCode.NoContent, client.call(HttpMethod.Delete, "/api/bets/42").status) }
        assertEquals(listOf<Any?>(BetId(42)), calls.received)
    }
}
