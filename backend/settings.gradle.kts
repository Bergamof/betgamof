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
// `inbound`, `outbound` and `outbound:persistence` only group adapters: one submodule per technology.
include(
    "business",
    "inbound:rest",
    "outbound:persistence:sqlite",
    "outbound:odds",
    "application",
)
