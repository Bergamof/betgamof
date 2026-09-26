// Use cases and ports. Depends on the domain only; adapters plug into the ports.
plugins {
    id("betgamof.kotlin-conventions")
    `java-test-fixtures`
}

dependencies {
    api(project(":domain"))

    // Test fixtures shared with the adapters: in-memory fakes of the driven ports and the contract
    // suites every implementation of a port must pass.
    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter)
}
