package fr.bergamof.betgamof.domain

import java.time.Instant

val T0: Instant = Instant.parse("2026-09-01T18:00:00Z")

fun bet(
    id: Long,
    odds: Double,
    stake: Money,
    status: BetStatus,
    bankrollId: Long = 1,
    montanteId: Long? = null,
    placedAt: Instant = T0.plusSeconds(id * 3_600),
    sport: String = "Football",
    cashout: Money? = null,
) = Bet(
    id = id,
    bankrollId = bankrollId,
    montanteId = montanteId,
    type = BetType.SIMPLE,
    bookmaker = "Winamax",
    stake = stake,
    odds = odds,
    status = status,
    cashout = cashout,
    selections = listOf(Selection(null, "Lens – Lyon", sport, "Ligue 1", "Total de buts", "Plus de 1,5", odds)),
    placedAt = placedAt,
    startsAt = placedAt.plusSeconds(1_800),
    settledAt = if (status == BetStatus.OPEN) null else placedAt.plusSeconds(3_000),
)

fun montanteConfig(
    mode: MontanteMode = MontanteMode.OBJECTIVE,
    targetMultiplier: Double? = 3.0,
    stepCount: Int? = null,
    excludeStake: Boolean = true,
    securePct: Int = 30,
    relancesAllowed: Int = 0,
) = MontanteConfig(
    name = "Montante Ligue 1",
    bankrollId = 1,
    startCapital = Money.euros(160),
    targetOdds = 1.75,
    mode = mode,
    targetMultiplier = targetMultiplier,
    stepCount = stepCount,
    excludeStake = excludeStake,
    securePct = securePct,
    relancesAllowed = relancesAllowed,
)

fun montante(
    config: MontanteConfig = montanteConfig(),
    closedAt: Instant? = null,
) = Montante(id = 10, config = config, createdAt = T0, closedAt = closedAt)
