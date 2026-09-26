package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.NotFoundException
import fr.bergamof.betgamof.domain.InvalidTransitionException
import fr.bergamof.betgamof.domain.InvalidValueException
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErrorMappingTest {
    private fun failingWith(failure: Throwable) = Calls().apply { this.failure = failure }

    @Test
    fun `an invalid value is a bad request`() =
        withApi(failingWith(InvalidValueException("La mise doit être positive"))) {
            assertError(HttpStatusCode.BadRequest, "La mise doit être positive", client.call(HttpMethod.Get, "/api/bets/1"))
        }

    @Test
    fun `a forbidden transition is a conflict`() =
        withApi(failingWith(InvalidTransitionException("Ce pari est déjà réglé"))) {
            val response = client.call(HttpMethod.Post, "/api/bets/1/settlement", """{"status":"WON"}""")
            assertError(HttpStatusCode.Conflict, "Ce pari est déjà réglé", response)
        }

    @Test
    fun `a missing resource is not found`() =
        withApi(failingWith(NotFoundException("Bankroll 9 introuvable"))) {
            assertError(HttpStatusCode.NotFound, "Bankroll 9 introuvable", client.call(HttpMethod.Get, "/api/bankrolls/9"))
        }

    @Test
    fun `an unexpected IllegalArgumentException is an internal error that hides its message`() =
        withApi(failingWith(IllegalArgumentException("secret detail"))) {
            assertError(HttpStatusCode.InternalServerError, "Erreur interne", client.call(HttpMethod.Get, "/api/rules"))
        }

    @Test
    fun `malformed bodies are bad requests`() {
        val calls = Calls()
        withApi(calls) {
            val malformed = client.call(HttpMethod.Post, "/api/rules", """{"kind":""")
            assertEquals(HttpStatusCode.BadRequest, malformed.status)

            val missingField = client.call(HttpMethod.Post, "/api/bankrolls", """{"name":"B","color":"CIEL"}""")
            assertEquals(HttpStatusCode.BadRequest, missingField.status)
            assertTrue("initialBalance" in missingField.jsonObject().getValue("message").toString())

            // Body enums are case-sensitive, as the frontend always sends the exact spelling.
            val unknownEnum = client.call(HttpMethod.Post, "/api/bankrolls", """{"name":"B","color":"ciel","initialBalance":1}""")
            assertEquals(HttpStatusCode.BadRequest, unknownEnum.status)
            assertTrue("ciel" in unknownEnum.jsonObject().getValue("message").toString())
        }
        assertTrue(calls.received.isEmpty())
    }

    @Test
    fun `malformed parameters are bad requests`() {
        val calls = Calls()
        withApi(calls) {
            assertError(HttpStatusCode.BadRequest, "Identifiant invalide", client.call(HttpMethod.Get, "/api/bets/abc"))
            assertError(HttpStatusCode.BadRequest, "Identifiant invalide", client.call(HttpMethod.Delete, "/api/rules/1.5"))
            assertError(HttpStatusCode.BadRequest, "Paramètre status invalide", client.call(HttpMethod.Get, "/api/bets?status=PENDING"))
            assertError(HttpStatusCode.BadRequest, "Paramètre bankrollId invalide", client.call(HttpMethod.Get, "/api/bets?bankrollId=x"))
            assertError(HttpStatusCode.BadRequest, "Paramètre lastDays invalide", client.call(HttpMethod.Get, "/api/bets?lastDays=1.5"))
            assertError(HttpStatusCode.BadRequest, "Paramètre period invalide", client.call(HttpMethod.Get, "/api/stats?period=YEAR"))
            assertError(HttpStatusCode.BadRequest, "Paramètre odds invalide", client.call(HttpMethod.Get, "/api/bankrolls/7/kelly"))
            assertError(HttpStatusCode.BadRequest, "Paramètre odds invalide", client.call(HttpMethod.Get, "/api/bankrolls/7/kelly?odds=x"))
        }
        assertEquals(emptyList(), calls.received)
    }
}
