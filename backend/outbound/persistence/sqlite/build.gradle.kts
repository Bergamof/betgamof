// Outbound adapter: SQLite storage (Exposed + Flyway) implementing the repository ports.
dependencies {
    implementation(project(":business"))
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.flyway.core)
    implementation(libs.sqlite.jdbc)
}
