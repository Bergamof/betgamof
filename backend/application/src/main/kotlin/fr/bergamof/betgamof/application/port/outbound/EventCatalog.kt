package fr.bergamof.betgamof.application.port.outbound

import fr.bergamof.betgamof.domain.SportEvent

/** Driven port: source of upcoming events and their odds. Implemented by adapters/odds. */
fun interface EventCatalog {
    fun upcoming(): List<SportEvent>
}
