// Driving adapter: fills an empty installation with demo data through the use cases.
plugins {
    id("betgamof.kotlin-conventions")
}

dependencies {
    implementation(project(":application"))

    testImplementation(testFixtures(project(":application")))
}
