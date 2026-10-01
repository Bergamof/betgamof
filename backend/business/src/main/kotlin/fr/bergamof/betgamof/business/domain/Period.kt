package fr.bergamof.betgamof.business.domain

import java.time.Duration
import java.time.Instant

/** A time range, open on either side: [from] included, [until] excluded. */
data class Period(
    val from: Instant? = null,
    val until: Instant? = null,
) {
    init {
        require(from == null || until == null || from <= until) { "Le début de la période doit précéder sa fin" }
    }

    operator fun contains(instant: Instant) = (from == null || instant >= from) && (until == null || instant < until)

    companion object {
        val ALL = Period()

        /** The [duration] leading up to [now]. */
        fun last(
            duration: Duration,
            now: Instant,
        ) = Period(from = now - duration)
    }
}
