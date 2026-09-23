// Shared setup of every backend module: Kotlin/JVM 21, ktlint, detekt and JUnit 5.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

group = "fr.bergamof"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("detekt.yml"))
}

val libs = versionCatalogs.named("libs")

dependencies {
    "testImplementation"(kotlin("test"))
    "testImplementation"(platform(libs.findLibrary("junit-bom").get()))
    "testImplementation"(libs.findLibrary("junit-jupiter").get())
    "testRuntimeOnly"(libs.findLibrary("junit-launcher").get())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
