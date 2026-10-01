package fr.bergamof.betgamof.business.service

import fr.bergamof.betgamof.business.port.inbound.EventUseCases
import fr.bergamof.betgamof.business.port.outbound.EventCatalog

class EventService(
    private val catalog: EventCatalog,
) : EventUseCases {
    override fun upcoming(query: String?) =
        catalog.upcoming().filter { query.isNullOrBlank() || it.name.contains(query.trim(), ignoreCase = true) }
}
