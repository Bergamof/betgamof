import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
}

/** Minimum line coverage of each module, checked by `koverVerify` (part of `check`). */
val minimumCoveragePercent = 80

// Shared setup of every module with a build script (`inbound`, `outbound` and `outbound/persistence` only group adapters):
// Kotlin/JVM 21, ktlint, detekt, JUnit 5 and a coverage threshold.
configure(subprojects.filter { it.buildFile.exists() }) {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jetbrains.kotlinx.kover")

    group = "fr.bergamof"
    version = "0.1.0"

    configure<KotlinJvmProjectExtension> {
        jvmToolchain(21)
    }

    configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("detekt.yml"))
    }

    configure<KoverProjectExtension> {
        reports {
            verify {
                rule { minBound(minimumCoveragePercent) }
            }
        }
    }

    dependencies {
        "testImplementation"(kotlin("test"))
        "testImplementation"(platform(rootProject.libs.junit.bom))
        "testImplementation"(rootProject.libs.junit.jupiter)
        "testRuntimeOnly"(rootProject.libs.junit.launcher)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.named("check") {
        dependsOn("koverVerify")
    }
}
