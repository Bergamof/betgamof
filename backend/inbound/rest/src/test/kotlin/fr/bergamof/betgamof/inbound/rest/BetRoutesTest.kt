package fr.bergamof.betgamof.inbound.rest

import fr.bergamof.betgamof.business.domain.BetChange
import fr.bergamof.betgamof.business.domain.BetStatus
import fr.bergamof.betgamof.business.domain.BetType
import fr.bergamof.betgamof.business.domain.NewBet
import fr.bergamof.betgamof.business.domain.Selection
import fr.bergamof.betgamof.business.domain.Settlement
import fr.bergamof.betgamof.business.port.inbound.BetFilter
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.justRun
import kotlin.test.Test
import kotlin.test.assertEquals

class BetRoutesTest : RestTest() {
    @Test
    fun `bets are listed without filter by default`() =
        api {
            every { bets.list(BetFilter()) } returns listOf(betView)

            val bet = client.call(HttpMethod.Get, "/api/bets").firstOfList()

            assertEquals("Lens – Lyon · Plus de 1,5", bet.at("label"))
            assertEquals("2026-09-11T19:00:00Z", bet.at("startsAt"))
            assertEquals("Plus de 1,5", bet.at("selections.0.pick"))
        }

    @Test
    fun `query parameters become a bet filter`() =
        api {
            val filter = BetFilter(BetStatus.OPEN, 2, "Tennis", "Unibet", 30, "nadal")
            every { bets.list(filter) } returns emptyList()

            val response =
                client.call(
                    HttpMethod.Get,
                    "/api/bets?status=open&bankrollId=2&sport=Tennis&bookmaker=Unibet&lastDays=30&q=nadal",
                )

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `blank sport and bookmaker are ignored`() =
        api {
            every { bets.list(BetFilter()) } returns emptyList()

            assertEquals(HttpStatusCode.OK, client.call(HttpMethod.Get, "/api/bets?sport=&bookmaker=").status)
        }

    @Test
    fun `an unknown status is a bad request`() =
        api {
            val response = client.call(HttpMethod.Get, "/api/bets?status=PENDING")

            assertEquals("Paramètre status invalide", response.jsonObject().at("message"))
        }

    @Test
    fun `a non numeric bankroll filter is a bad request`() =
        api {
            val response = client.call(HttpMethod.Get, "/api/bets?bankrollId=x")

            assertEquals("Paramètre bankrollId invalide", response.jsonObject().at("message"))
        }

    @Test
    fun `a bet is created from its request`() =
        api {
            val expected =
                NewBet(
                    bankrollId = 1,
                    montanteId = null,
                    type = BetType.SIMPLE,
                    bookmaker = "Winamax",
                    stake = euros(25.0),
                    odds = 1.72,
                    startsAt = AT,
                    selections = listOf(Selection(null, "Lens – Lyon", "Football", "", "", "Plus de 1,5", 1.72)),
                )
            every { bets.create(expected) } returns betView
            val body =
                """{"bankrollId":1,"type":"SIMPLE","bookmaker":"Winamax","stake":25,"odds":1.72,"startsAt":"2026-09-11T19:00:00Z",
                   "selections":[{"eventName":"Lens – Lyon","sport":"Football","pick":"Plus de 1,5","odds":1.72}]}"""

            assertEquals(HttpStatusCode.Created, client.call(HttpMethod.Post, "/api/bets", body).status)
        }

    @Test
    fun `a bet is read by id`() =
        api {
            every { bets.get(7) } returns betView

            assertEquals("18.0", client.call(HttpMethod.Get, "/api/bets/7").jsonObject().at("profit"))
        }

    @Test
    fun `a bet is updated with a bet change`() =
        api {
            every { bets.update(7, BetChange(euros(30.0), 1.9, "Unibet")) } returns betView

            val response = client.call(HttpMethod.Put, "/api/bets/7", """{"stake":30,"odds":1.9,"bookmaker":"Unibet"}""")

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `a bet is settled with an optional cash-out`() =
        api {
            every { bets.settle(7, Settlement(BetStatus.CASHOUT, euros(12.5))) } returns betView

            val response = client.call(HttpMethod.Post, "/api/bets/7/settlement", """{"status":"CASHOUT","cashout":12.5}""")

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `a deleted bet answers no content`() =
        api {
            justRun { bets.delete(7) }

            assertEquals(HttpStatusCode.NoContent, client.call(HttpMethod.Delete, "/api/bets/7").status)
        }
}
