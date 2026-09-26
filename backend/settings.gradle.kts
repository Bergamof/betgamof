rootProject.name = "betgamof-backend"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// Hexagonal architecture: dependencies only point inwards (adapters → application → domain).
include(
    "domain",
    "application",
    "adapters:http",
    "adapters:persistence",
    "adapters:odds",
    "app",
)
