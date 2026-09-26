// Use cases and ports. Depends on the domain only; adapters plug into the ports.
plugins {
    id("betgamof.kotlin-conventions")
}

dependencies {
    api(project(":domain"))
}
