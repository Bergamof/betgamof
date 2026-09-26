package fr.bergamof.betgamof.adapter.http

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthTest {
    @Test
    fun `health is public`() =
        withApi {
            val response = client.get("/health")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("ok", response.bodyAsText())
        }

    @Test
    fun `api routes require the token`() =
        withApi(Calls()) {
            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/bankrolls").status)
            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/bankrolls") { bearerAuth("wrong-token") }.status)
        }

    @Test
    fun `rejected requests never reach the use cases`() {
        val calls = Calls()
        withApi(calls) { client.get("/api/bets") { bearerAuth("wrong-token") } }
        assertTrue(calls.received.isEmpty())
    }
}
