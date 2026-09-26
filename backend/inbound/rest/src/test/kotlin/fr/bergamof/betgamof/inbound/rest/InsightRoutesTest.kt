package fr.bergamof.betgamof.inbound.rest

import fr.bergamof.betgamof.business.domain.NewRule
import fr.bergamof.betgamof.business.domain.RuleKind
import fr.bergamof.betgamof.business.port.inbound.StatsPeriod
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.justRun
import kotlin.test.Test
import kotlin.test.assertEquals

class InsightRoutesTest : RestTest() {
    @Test
    fun `stats cover the whole history by default`() =
        api {
            every { insights.stats(StatsPeriod.ALL, null) } returns statsView

            val stats = client.call(HttpMethod.Get, "/api/stats").jsonObject()

            assertEquals("0.5", stats.at("hitRate"))
            assertEquals("false", stats.at("currentStreak.won"))
            assertEquals("4.0", stats.at("profitCurve.0.cumulativeProfit"))
            assertEquals("Football", stats.at("bySport.0.label"))
            assertEquals("null", stats.at("montantes.securedCoverage"))
        }

    @Test
    fun `stats can target a period and a bankroll`() =
        api {
            every { insights.stats(StatsPeriod.DAYS_30, 2) } returns statsView

            assertEquals(HttpStatusCode.OK, client.call(HttpMethod.Get, "/api/stats?period=days_30&bankrollId=2").status)
        }

    @Test
    fun `rules are listed with their state`() =
        api {
            every { insights.rules() } returns listOf(ruleView)

            val rule = client.call(HttpMethod.Get, "/api/rules").firstOfList()

            assertEquals("MAX_STAKE_PCT", rule.at("kind"))
            assertEquals("true", rule.at("respected"))
        }

    @Test
    fun `a rule is added and the rules are returned`() =
        api {
            every { insights.addRule(NewRule(RuleKind.SINGLE_ACTIVE_MONTANTE, 0)) } returns listOf(ruleView)

            val response = client.call(HttpMethod.Post, "/api/rules", """{"kind":"SINGLE_ACTIVE_MONTANTE"}""")

            assertEquals(HttpStatusCode.Created, response.status)
        }

    @Test
    fun `a rule without its required parameter is a bad request`() =
        api {
            val response = client.call(HttpMethod.Post, "/api/rules", """{"kind":"MAX_STAKE_PCT"}""")

            assertEquals("Le paramètre de la règle doit être positif", response.jsonObject().at("message"))
        }

    @Test
    fun `a deleted rule answers no content`() =
        api {
            justRun { insights.deleteRule(1) }

            assertEquals(HttpStatusCode.NoContent, client.call(HttpMethod.Delete, "/api/rules/1").status)
        }

    @Test
    fun `upcoming events are searched by name`() =
        api {
            every { events.upcoming("psg") } returns listOf(sportEvent)

            val event = client.call(HttpMethod.Get, "/api/events?q=psg").firstOfList()

            assertEquals("PSG – OM", event.at("name"))
            assertEquals("1.6", event.at("markets.0.outcomes.0.odds"))
        }
}
