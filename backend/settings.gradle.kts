rootProject.name = "betgamof-backend"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// Hexagonal architecture: `business` is the core; adapters and the application only point inwards.
include(
    "business",
    "inbound:rest",
    "outbound:persistence",
    "outbound:odds",
    "application",
)
