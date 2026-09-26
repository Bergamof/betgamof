// Driven adapter: source of sport events and odds (simulated until a real provider is plugged in).
plugins {
    id("betgamof.kotlin-conventions")
}

dependencies {
    implementation(project(":application"))
}
