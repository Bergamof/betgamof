// Driven adapter: SQLite storage (Exposed + Flyway) implementing the repository ports.
plugins {
    id("betgamof.kotlin-conventions")
}

dependencies {
    implementation(project(":application"))
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.flyway.core)
    implementation(libs.sqlite.jdbc)

    // Contract suites shared with the in-memory fakes.
    testImplementation(testFixtures(project(":application")))
}
