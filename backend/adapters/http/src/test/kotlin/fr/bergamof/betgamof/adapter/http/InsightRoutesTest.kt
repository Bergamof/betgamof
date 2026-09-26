package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.port.inbound.AddRuleCommand
import fr.bergamof.betgamof.application.port.inbound.StatsPeriod
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.RuleId
import fr.bergamof.betgamof.domain.RuleKind
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class InsightRoutesTest {
    @Test
    fun `serializes statistics with segment labels`() =
        withApi {
            val response = client.call(HttpMethod.Get, "/api/stats")
            assertEquals(HttpStatusCode.OK, response.status)
            val stats = response.jsonObject()

            fun labels(name: String) =
                stats.getValue(name).jsonArray.map {
                    it.jsonObject
                        .getValue("label")
                        .jsonPrimitive.content
                }

            fun label(name: String) =
                stats
                    .getValue(name)
                    .jsonObject
                    .getValue("label")
                    .jsonPrimitive.content
            assertEquals(listOf("Tennis"), labels("bySport"))
            assertEquals(listOf("1.0 – 1.5", "1.5 – 2.0", "2.0 – 3.0", "3.0 +"), labels("byOddsRange"))
            assertEquals(listOf("Buts", "Combiné"), labels("byMarket"))
            assertEquals("late-night", label("lateNight"))
            assertEquals("day-before", label("placedDayBefore"))
            assertJson("""{"label":"late-night","count":2,"staked":20.0,"profit":4.0,"yield":0.2}""", stats.getValue("lateNight"))
            val scalars = stats.filterKeys { it !in setOf("bySport", "byOddsRange", "byMarket", "lateNight", "placedDayBefore") }
            assertJson(
                """
                {"settledCount":2,"openCount":1,"staked":20.0,"profit":4.0,"yield":0.2,"won":1,"lost":1,"hitRate":0.5,
                 "averageOdds":2.2,"breakEvenOdds":2.0,"averageStake":10.0,"averageStakeShare":0.01,"bestWinStreak":1,
                 "currentStreak":{"won":false,"length":1},"maxDrawdown":10.0,"firstBetAt":"2026-09-10T20:00:00Z",
                 "profitCurve":[{"at":"2026-09-11T19:00:00Z","cumulativeProfit":4.0}],
                 "montantes":{"launched":1,"succeeded":0,"broken":1,"active":0,"closed":0,"averagePaliersReached":1.0,
                              "securedCoverage":null}}
                """,
                JsonObject(scalars),
            )
        }

    @Test
    fun `parses the stats period case-insensitively, defaulting to all`() {
        val calls = Calls()
        withApi(calls) {
            client.call(HttpMethod.Get, "/api/stats")
            client.call(HttpMethod.Get, "/api/stats?period=days_30&bankrollId=7")
            client.call(HttpMethod.Get, "/api/stats?period=MONTHS_3")
        }
        assertEquals(
            listOf<Any?>(StatsPeriod.ALL, null, StatsPeriod.DAYS_30, BankrollId(7), StatsPeriod.MONTHS_3, null),
            calls.received,
        )
    }

    @Test
    fun `manages discipline rules`() {
        val calls = Calls()
        withApi(calls) {
            val rule = """[{"id":5,"kind":"PAUSE_AFTER_LOSSES","param":3,"respected":true}]"""
            assertJson(rule, client.call(HttpMethod.Get, "/api/rules").json())
            val added = client.call(HttpMethod.Post, "/api/rules", """{"kind":"SINGLE_ACTIVE_MONTANTE"}""")
            assertEquals(HttpStatusCode.Created, added.status)
            assertJson(rule, added.json())
            assertEquals(HttpStatusCode.NoContent, client.call(HttpMethod.Delete, "/api/rules/5").status)
        }
        assertEquals(listOf<Any?>(AddRuleCommand(RuleKind.SINGLE_ACTIVE_MONTANTE, 0), RuleId(5)), calls.received)
    }

    @Test
    fun `lists upcoming events`() {
        val calls = Calls()
        withApi(calls) {
            val response = client.call(HttpMethod.Get, "/api/events?q=lens")
            assertEquals(HttpStatusCode.OK, response.status)
            assertJson(
                """
                [{"id":"evt-1","sport":"Football","competition":"Ligue 1","name":"Lens – Lyon","startsAt":"2026-09-11T19:00:00Z",
                  "markets":[{"id":"m-1","name":"Mi-temps","category":"MI_TEMPS","popular":false,
                              "outcomes":[{"pick":"Lens","odds":2.4,"bookmaker":"Unibet"}]}]}]
                """,
                response.json(),
            )
        }
        assertEquals(listOf<Any?>("lens"), calls.received)
    }
}
