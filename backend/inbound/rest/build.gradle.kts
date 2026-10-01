// Inbound adapter: REST API (Ktor). Maps JSON DTOs to domain objects and calls the business inbound ports.
plugins {
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    implementation(project(":business"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.json)
    implementation(libs.serialization.json)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.mockk)
}
