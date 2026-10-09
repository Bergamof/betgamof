package fr.bergamof.betgamof.business.domain

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Every invariant of the domain objects, each checked on its own: expected message → invalid object. */
class ValidationTest {
    private val selection = Selection(null, "Lens – Lyon", "Football", "Ligue 1", "Résultat", "Lens", 1.8)
    private val simple = NewBet(1, null, BetType.SIMPLE, "Winamax", Money.euros(10), 1.8, T0, listOf(selection))

    private fun rejects(cases: Map<String, () -> Any>) =
        cases.map { (message, build) ->
            dynamicTest(message) { assertEquals(message, assertFailsWith<IllegalArgumentException> { build() }.message) }
        }

    @TestFactory
    fun `bankroll settings`() =
        rejects(
            mapOf(
                "Le nom de la bankroll est obligatoire" to
                    { BankrollSettings(" ", BankrollColor.GAZON, Money.euros(1), null, 0.25, null) },
                "Le solde initial doit être positif" to
                    { BankrollSettings("B", BankrollColor.GAZON, Money.euros(-1), null, 0.25, null) },
                "La fraction de Kelly doit être comprise entre 0 et 1" to
                    { BankrollSettings("B", BankrollColor.GAZON, Money.euros(1), null, 1.5, null) },
                "La mise par défaut doit être positive" to { DefaultStake.Amount(Money.ZERO) },
                "Le pourcentage de mise doit être compris entre 0 et 100" to { DefaultStake.Percent(0.0) },
            ),
        )

    @TestFactory
    fun selections() =
        rejects(
            mapOf(
                "L'événement est obligatoire" to { selection.copy(eventName = "") },
                "La sélection est obligatoire" to { selection.copy(pick = " ") },
                "Une cote doit être supérieure à 1" to { selection.copy(odds = 1.0) },
            ),
        )

    @TestFactory
    fun bets() =
        rejects(
            mapOf(
                "La mise doit être positive" to { simple.copy(stake = Money.ZERO) },
                "La cote doit être supérieure à 1" to { simple.copy(odds = 1.0) },
                "Le bookmaker est obligatoire" to { simple.copy(bookmaker = "") },
                "Un pari contient au moins une sélection" to { simple.copy(selections = emptyList()) },
                "Un pari simple contient une seule sélection" to { simple.copy(selections = listOf(selection, selection)) },
                "Un combiné ou un système contient plusieurs sélections" to { simple.copy(type = BetType.COMBINE) },
            ),
        )

    @TestFactory
    fun `bet changes`() =
        rejects(
            mapOf(
                "La mise doit être positive" to { BetChange(Money.ZERO, 2.0, "Winamax") },
                "La cote doit être supérieure à 1" to { BetChange(Money.euros(1), 0.5, "Winamax") },
                "Le bookmaker est obligatoire" to { BetChange(Money.euros(1), 2.0, " ") },
            ),
        )

    @TestFactory
    fun settlements() =
        rejects(
            mapOf(
                "Un règlement ne peut pas remettre le pari en cours" to { Settlement(BetStatus.OPEN, null) },
                "Le montant du cash-out est obligatoire" to { Settlement(BetStatus.CASHOUT, null) },
            ),
        )

    @TestFactory
    fun `montante configurations`() =
        rejects(
            mapOf(
                "Le nom de la montante est obligatoire" to { montanteConfig().copy(name = "") },
                "Le capital de départ doit être positif" to { montanteConfig().copy(startCapital = Money.ZERO) },
                "La cote visée doit être supérieure à 1" to { montanteConfig().copy(targetOdds = 1.0) },
                "La part sécurisée doit être comprise entre 0 et 90 %" to { montanteConfig(securePct = 95) },
                "Le nombre de relances doit être compris entre 0 et 10" to { montanteConfig(relancesAllowed = 11) },
                "L'objectif doit être supérieur au capital de départ" to { montanteConfig(targetMultiplier = 1.0) },
                "Le nombre de paliers doit être au moins 1" to { montanteConfig(mode = MontanteMode.STEPS, stepCount = 0) },
            ),
        )

    @TestFactory
    fun rules() =
        rejects(
            mapOf(
                "Le paramètre de la règle doit être positif" to { NewRule(RuleKind.MAX_STAKE_PCT, 0) },
            ),
        )

    @TestFactory
    fun `valid objects are accepted`() =
        mapOf(
            "rule without parameter" to { NewRule(RuleKind.SINGLE_ACTIVE_MONTANTE, 0) },
            "free montante" to { montanteConfig(mode = MontanteMode.FREE, targetMultiplier = null) },
            "cash-out with amount" to { Settlement(BetStatus.CASHOUT, Money.euros(5)) },
        ).map { (name, build) -> dynamicTest(name) { build() } }
}
