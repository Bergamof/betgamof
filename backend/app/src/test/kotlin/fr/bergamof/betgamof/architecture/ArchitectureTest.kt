package fr.bergamof.betgamof.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

private const val PROJECT = "fr.bergamof.betgamof"
private const val DOMAIN = "$PROJECT.domain"
private const val APPLICATION = "$PROJECT.application"
private const val MODEL = "$APPLICATION.model"
private const val NOT_FOUND = "$APPLICATION.NotFoundException"
private const val PORT = "$APPLICATION.port"
private const val INBOUND_PORT = "$PORT.inbound"
private const val OUTBOUND_PORT = "$PORT.outbound"
private const val SERVICE = "$APPLICATION.service"
private const val ADAPTER = "$PROJECT.adapter"
private const val COMPOSITION_ROOT_MODULE = "app"
private val DRIVING_ADAPTERS = listOf("$ADAPTER.http", "$ADAPTER.demo")
private val DRIVEN_ADAPTERS = listOf("$ADAPTER.persistence", "$ADAPTER.odds")

/**
 * Hexagonal architecture rules, checked on the imports of every production source file of the backend
 * (test sources and test fixtures are out of scope).
 */
class ArchitectureTest {
    @Test
    fun `the domain depends on nothing but the JDK and the Kotlin library`() {
        filesIn(DOMAIN).assertTrue(strict = true) { file ->
            file.projectImports().all { it.isIn(DOMAIN) } && file.foreignImports().all { it.isIn("java") || it.isIn("kotlin") }
        }
    }

    @Test
    fun `the application depends on the domain only, never on adapters or frameworks`() {
        filesIn(APPLICATION).assertTrue(strict = true) { file ->
            file.projectImports().all { it.isIn(DOMAIN) || it.isIn(APPLICATION) } &&
                file.foreignImports().all { it.isIn("java") || it.isIn("kotlin") }
        }
    }

    @Test
    fun `ports do not know the services implementing them`() {
        filesIn(PORT).assertTrue(strict = true) { file -> file.projectImports().none { it.isIn(SERVICE) } }
    }

    @Test
    fun `driving adapters only use the inbound ports, their read models and the domain`() {
        val allowed = listOf(DOMAIN, INBOUND_PORT, MODEL, NOT_FOUND)
        DRIVING_ADAPTERS.forEach { adapter ->
            filesIn(adapter).assertTrue(strict = true, additionalMessage = adapter) { file ->
                file.projectImports().all { import -> import.isIn(adapter) || allowed.any { import.isIn(it) } }
            }
        }
    }

    @Test
    fun `driven adapters only use the outbound ports and the domain`() {
        val allowed = listOf(DOMAIN, OUTBOUND_PORT)
        DRIVEN_ADAPTERS.forEach { adapter ->
            filesIn(adapter).assertTrue(strict = true, additionalMessage = adapter) { file ->
                file.projectImports().all { import -> import.isIn(adapter) || allowed.any { import.isIn(it) } }
            }
        }
    }

    @Test
    fun `adapters do not know each other`() {
        (DRIVING_ADAPTERS + DRIVEN_ADAPTERS).forEach { adapter ->
            filesIn(adapter).assertTrue(strict = true, additionalMessage = adapter) { file ->
                file.projectImports().none { it.isIn(ADAPTER) && !it.isIn(adapter) }
            }
        }
    }

    @Test
    fun `only the composition root instantiates the services`() {
        production.files
            .filter { file -> file.projectImports().any { it.isIn(SERVICE) } }
            .assertTrue(strict = true) { file -> file.moduleName == COMPOSITION_ROOT_MODULE || file.packageName.isIn(SERVICE) }
    }
}

/** Parsed once: every rule reads the same production sources. */
private val production by lazy { Konsist.scopeFromProduction() }

/** True when this qualified name is [prefix] itself or lies inside it. */
private fun String.isIn(prefix: String) = this == prefix || startsWith("$prefix.")

private val KoFileDeclaration.packageName get() = packagee?.name.orEmpty()

private fun filesIn(pkg: String) = production.files.filter { it.packageName.isIn(pkg) }

/** Imports of the backend's own code. */
private fun KoFileDeclaration.projectImports() = imports.map { it.name }.filter { it.isIn(PROJECT) }

/** Imports of libraries: the JDK, Kotlin and third-party frameworks. */
private fun KoFileDeclaration.foreignImports() = imports.map { it.name }.filterNot { it.isIn(PROJECT) }
