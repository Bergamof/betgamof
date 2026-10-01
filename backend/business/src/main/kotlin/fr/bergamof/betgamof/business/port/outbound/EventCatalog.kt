package fr.bergamof.betgamof.business.port.outbound

import fr.bergamof.betgamof.business.domain.SportEvent

/** Driven port: source of upcoming events and their odds. Implemented by outbound/odds. */
fun interface EventCatalog {
    fun upcoming(): List<SportEvent>
}
