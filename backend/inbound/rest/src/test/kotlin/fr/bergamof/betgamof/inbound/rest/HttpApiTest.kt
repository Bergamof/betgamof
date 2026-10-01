package fr.bergamof.betgamof.inbound.rest

import fr.bergamof.betgamof.business.ConflictException
import fr.bergamof.betgamof.business.NotFoundException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.mockk.every
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpApiTest : RestTest() {
    @Test
    fun `the health probe is public`() =
        api {
            val response = client.get("/health")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("ok", response.bodyAsText())
        }

    @Test
    fun `requests without a token are unauthorized`() =
        api {
            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/bankrolls").status)
        }

    @Test
    fun `requests with a wrong token are unauthorized`() =
        api {
            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/bankrolls") { bearerAuth("wrong") }.status)
        }

    @Test
    fun `a missing resource is a 404 with its message`() =
        api {
            every { bankrolls.get(42) } throws NotFoundException("Bankroll 42 introuvable")

            val response = client.call(HttpMethod.Get, "/api/bankrolls/42")

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("Bankroll 42 introuvable", response.jsonObject().at("message"))
        }

    @Test
    fun `a state conflict is a 409 with its message`() =
        api {
            every { bankrolls.delete(1) } throws ConflictException("Bankroll utilisée")

            val response = client.call(HttpMethod.Delete, "/api/bankrolls/1")

            assertEquals(HttpStatusCode.Conflict, response.status)
            assertEquals("Bankroll utilisée", response.jsonObject().at("message"))
        }

    @Test
    fun `a broken business rule is a 400 with its message`() =
        api {
            val response = client.call(HttpMethod.Post, "/api/bankrolls", """{"name":" ","color":"GAZON","initialBalance":10}""")

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("Le nom de la bankroll est obligatoire", response.jsonObject().at("message"))
        }

    @Test
    fun `an unreadable body is a 400`() =
        api {
            val response = client.call(HttpMethod.Post, "/api/bankrolls", """{"name":"B"}""")

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `an invalid identifier is a 400`() =
        api {
            val response = client.call(HttpMethod.Get, "/api/bankrolls/abc")

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("Identifiant invalide", response.jsonObject().at("message"))
        }

    @Test
    fun `an unexpected error is a 500 without details`() =
        api {
            every { bankrolls.list() } throws IllegalStateException("boom")

            val response = client.call(HttpMethod.Get, "/api/bankrolls")

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertEquals("Erreur interne", response.jsonObject().at("message"))
        }
}
