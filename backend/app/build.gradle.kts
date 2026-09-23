// Composition root: configuration, wiring of adapters to use cases, entry point.
plugins {
    id("betgamof.kotlin-conventions")
    application
}

application {
    mainClass.set("fr.bergamof.betgamof.ApplicationKt")
    applicationName = "betgamof-backend"
}

dependencies {
    implementation(project(":application"))
    implementation(project(":adapters:http"))
    implementation(project(":adapters:persistence"))
    implementation(project(":adapters:odds"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.serialization.json)
}
