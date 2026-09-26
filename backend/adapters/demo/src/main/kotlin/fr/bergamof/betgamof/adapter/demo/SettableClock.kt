package fr.bergamof.betgamof.adapter.demo

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * A clock the demo seeder moves by hand. The use cases it drives are built with it, so the history
 * they record is dated in the past without the seeder writing timestamps itself.
 */
class SettableClock(
    start: Instant,
    private val zone: ZoneId = ZoneOffset.UTC,
) : Clock() {
    @Volatile
    private var now: Instant = start

    fun set(instant: Instant) {
        now = instant
    }

    override fun instant(): Instant = now

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = SettableClock(now, zone)
}
