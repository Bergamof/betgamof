package fr.bergamof.betgamof.inbound.rest

import fr.bergamof.betgamof.business.port.inbound.BankrollUseCases
import fr.bergamof.betgamof.business.port.inbound.BetUseCases
import fr.bergamof.betgamof.business.port.inbound.EventUseCases
import fr.bergamof.betgamof.business.port.inbound.InsightUseCases
import fr.bergamof.betgamof.business.port.inbound.MontanteUseCases
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val TOKEN = "test-token"

/** Runs the REST API on mocked inbound ports: tests only check the HTTP ↔ domain mapping. */
abstract class RestTest {
    protected val bankrolls = mockk<BankrollUseCases>()
    protected val bets = mockk<BetUseCases>()
    protected val montantes = mockk<MontanteUseCases>()
    protected val insights = mockk<InsightUseCases>()
    protected val events = mockk<EventUseCases>()

    protected fun api(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application { betgamofApi(UseCases(bankrolls, bets, montantes, insights, events), TOKEN) }
            block()
        }

    protected suspend fun HttpClient.call(
        method: HttpMethod,
        path: String,
        body: String? = null,
    ): HttpResponse =
        request(path) {
            this.method = method
            bearerAuth(TOKEN)
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }

    protected suspend fun HttpResponse.json(): JsonElement = Json.parseToJsonElement(bodyAsText())

    protected suspend fun HttpResponse.jsonObject(): JsonObject = json().jsonObject

    protected suspend fun HttpResponse.firstOfList(): JsonObject = json().jsonArray.first().jsonObject

    /** Value at a dotted path, list indexes included: `at("paliers.0.stake")`. */
    protected fun JsonObject.at(path: String): String =
        path
            .split('.')
            .fold(this as JsonElement) { element, key ->
                key.toIntOrNull()?.let { element.jsonArray[it] } ?: element.jsonObject.getValue(key)
            }.jsonPrimitive.content
}
