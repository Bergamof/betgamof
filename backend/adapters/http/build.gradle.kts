// Driving adapter: REST API (Ktor) calling the application's inbound ports.
plugins {
    id("betgamof.kotlin-conventions")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    implementation(project(":application"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.json)
    implementation(libs.serialization.json)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
}
