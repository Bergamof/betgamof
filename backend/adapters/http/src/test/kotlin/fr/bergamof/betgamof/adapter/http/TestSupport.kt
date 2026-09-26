package fr.bergamof.betgamof.adapter.http

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.assertEquals

const val TOKEN = "test-token"

/** Runs [block] against the API wired to fakes; [calls] records what the adapter passed to them. */
fun withApi(
    calls: Calls = Calls(),
    block: suspend ApplicationTestBuilder.() -> Unit,
) = testApplication {
    val useCases = UseCases(FakeBankrolls(calls), FakeBets(calls), FakeMontantes(calls), FakeInsights(calls), FakeEvents(calls))
    application { betgamofApi(useCases, TOKEN) }
    block()
}

suspend fun HttpClient.call(
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

suspend fun HttpResponse.json(): JsonElement = Json.parseToJsonElement(bodyAsText())

suspend fun HttpResponse.jsonObject(): JsonObject = json().jsonObject

/** Compares JSON structurally, but with numbers compared by their exact spelling (1.0 differs from 1). */
fun assertJson(
    expected: String,
    actual: JsonElement,
) = assertEquals(Json.parseToJsonElement(expected), actual)

suspend fun assertError(
    expectedStatus: HttpStatusCode,
    expectedMessage: String,
    response: HttpResponse,
) {
    assertEquals(expectedStatus, response.status)
    assertJson("""{"message":${JsonPrimitive(expectedMessage)}}""", response.json())
}
