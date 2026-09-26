package fr.bergamof.betgamof.application.service

import fr.bergamof.betgamof.application.port.inbound.EventUseCases
import fr.bergamof.betgamof.application.port.outbound.EventCatalog

class EventService(
    private val catalog: EventCatalog,
) : EventUseCases {
    override fun upcoming(query: String?) =
        catalog.upcoming().filter { query.isNullOrBlank() || it.name.contains(query.trim(), ignoreCase = true) }
}
