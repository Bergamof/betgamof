package fr.bergamof.betgamof.application.contract

import fr.bergamof.betgamof.domain.Bankroll
import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.BankrollSettings
import fr.bergamof.betgamof.domain.Bet
import fr.bergamof.betgamof.domain.BetId
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.Montante
import fr.bergamof.betgamof.domain.MontanteConfig
import fr.bergamof.betgamof.domain.MontanteId
import fr.bergamof.betgamof.domain.MontanteMode
import fr.bergamof.betgamof.domain.Selection
import java.time.Instant

/**
 * Base of the contract suites: every implementation of the driven persistence ports must pass them,
 * so the in-memory fakes used by the use case tests behave like the real database.
 * Subclasses provide a fresh, empty store; each test gets its own.
 */
abstract class PersistenceContract {
    /** A new, empty store. Called once per test. */
    protected abstract fun newPersistence(): PersistencePorts

    protected val ports: PersistencePorts by lazy { newPersistence() }

    /** Nanosecond precision on purpose: instants must come back exactly as they were stored. */
    protected val now: Instant = Instant.parse("2026-09-11T16:00:00.123456789Z")

    protected fun later(seconds: Long): Instant = now.plusSeconds(seconds)

    protected fun settings(
        name: String = "Principale",
        color: BankrollColor = BankrollColor.CIEL,
    ) = BankrollSettings.of(
        name = name,
        color = color,
        initialBalance = Money(cents = 108_850),
        stopLoss = Money(cents = 90_000),
        kellyFraction = 0.3,
        fixedStake = Money(cents = 500),
    )

    protected fun newBankroll(settings: BankrollSettings = settings()) = Bankroll(BankrollId.NEW, settings, now)

    protected fun addBankroll(name: String = "Principale") = ports.bankrolls.add(newBankroll(settings(name)))

    protected fun config(
        bankrollId: BankrollId,
        mode: MontanteMode = MontanteMode.OBJECTIVE,
    ) = MontanteConfig.of(
        name = "Montante $mode",
        bankrollId = bankrollId,
        startCapital = Money(cents = 16_000),
        targetOdds = 1.75,
        mode = mode,
        targetMultiplier = if (mode == MontanteMode.OBJECTIVE) 3.5 else null,
        stepCount = if (mode == MontanteMode.STEPS) 4 else null,
        excludeStake = mode != MontanteMode.FREE,
        securePct = 30,
        relancesAllowed = 2,
    )

    protected fun newMontante(
        bankrollId: BankrollId,
        mode: MontanteMode = MontanteMode.OBJECTIVE,
    ) = Montante(MontanteId.NEW, config(bankrollId, mode), now, null)

    protected fun selection(
        eventName: String,
        eventId: String? = "evt-$eventName",
        odds: Double = 1.5,
    ) = Selection.of(
        eventId = eventId,
        eventName = eventName,
        sport = "Football",
        competition = "Ligue 1",
        market = "Résultat final",
        pick = "$eventName gagne",
        odds = odds,
    )

    /** An open combined bet with three selections, one of them without catalog event. */
    protected fun newBet(
        bankrollId: BankrollId,
        montanteId: MontanteId? = null,
        type: BetType = BetType.COMBINE,
        selections: List<Selection> =
            listOf(selection("Lens – Lyon"), selection("Nadal – Federer", eventId = null, odds = 2.1), selection("PSG – OM")),
    ) = Bet(
        id = BetId.NEW,
        bankrollId = bankrollId,
        montanteId = montanteId,
        type = type,
        bookmaker = "Winamax",
        stake = Money(cents = 2_550),
        odds = 3.15,
        status = BetStatus.OPEN,
        cashout = null,
        selections = selections,
        placedAt = now,
        startsAt = later(seconds = 3_600),
        settledAt = null,
    )
}
