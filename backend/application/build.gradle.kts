// Application: configuration, wiring of the adapters to the business services, entry point.
plugins {
    application
}

application {
    mainClass.set("fr.bergamof.betgamof.application.ApplicationKt")
    applicationName = "betgamof-backend"
}

dependencies {
    implementation(project(":business"))
    implementation(project(":inbound:rest"))
    implementation(project(":outbound:persistence"))
    implementation(project(":outbound:odds"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback)

    testImplementation(testFixtures(project(":business")))
}

testing {
    suites {
        // Component tests: the whole application (REST API, services, SQLite) driven over HTTP.
        register<JvmTestSuite>("componentTest") {
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation(project())
                implementation("org.jetbrains.kotlin:kotlin-test-junit5")
                implementation(libs.ktor.server.test.host)
                implementation(libs.serialization.json)
            }
            targets.configureEach {
                testTask.configure { shouldRunAfter(tasks.named("test")) }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("componentTest"))
}
