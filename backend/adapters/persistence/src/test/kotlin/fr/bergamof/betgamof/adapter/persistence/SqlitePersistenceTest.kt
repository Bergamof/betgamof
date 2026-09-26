package fr.bergamof.betgamof.adapter.persistence

import fr.bergamof.betgamof.domain.Bankroll
import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.BankrollSettings
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.DisciplineRule
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteMode
import fr.bergamof.betgamof.domain.RuleKind
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.DriverManager
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SqlitePersistenceTest {
    @TempDir
    lateinit var dir: Path

    private val path get() = dir.resolve("test.db").toString()

    private val bankroll =
        Bankroll(
            BankrollId.NEW,
            BankrollSettings.of("Principale", BankrollColor.CIEL, Money.euros(100)),
            Instant.parse("2026-09-11T16:00:00Z"),
        )

    private fun execute(sql: String) {
        DriverManager.getConnection("jdbc:sqlite:$path").use { it.createStatement().use { statement -> statement.executeUpdate(sql) } }
    }

    @Test
    fun `stored enum codes are the ones existing databases contain`() {
        assertEquals(setOf("GAZON", "CIEL", "CITRON", "ORANGE", "BRIQUE"), bankrollColorCodes.codes)
        assertEquals(setOf("SIMPLE", "COMBINE", "SYSTEME"), betTypeCodes.codes)
        assertEquals(setOf("OPEN", "WON", "LOST", "VOID", "CASHOUT"), betStatusCodes.codes)
        assertEquals(setOf("OBJECTIVE", "STEPS", "FREE"), montanteModeCodes.codes)
        assertEquals(setOf("MAX_STAKE_PCT", "PAUSE_AFTER_LOSSES", "SINGLE_ACTIVE_MONTANTE"), ruleKindCodes.codes)
    }

    @Test
    fun `every constant round-trips through its stored code`() {
        BankrollColor.entries.forEach { assertEquals(it, bankrollColorCodes.decode(bankrollColorCodes.encode(it))) }
        BetType.entries.forEach { assertEquals(it, betTypeCodes.decode(betTypeCodes.encode(it))) }
        BetStatus.entries.forEach { assertEquals(it, betStatusCodes.decode(betStatusCodes.encode(it))) }
        MontanteMode.entries.forEach { assertEquals(it, montanteModeCodes.decode(montanteModeCodes.encode(it))) }
        RuleKind.entries.forEach { assertEquals(it, ruleKindCodes.decode(ruleKindCodes.encode(it))) }
    }

    @Test
    fun `an unknown stored code fails with the column and the value`() {
        val persistence = SqlitePersistence(path)
        persistence.bankrolls.add(bankroll)
        execute("UPDATE bankrolls SET color = 'VIOLET'")

        val error = assertFailsWith<IllegalStateException> { persistence.bankrolls.findAll() }

        assertEquals("Unknown code 'VIOLET' in column bankrolls.color", error.message)
    }

    @Test
    fun `an unknown rule kind fails with the column and the value`() {
        val persistence = SqlitePersistence(path)
        val rule = persistence.rules.add(DisciplineRule.define(RuleKind.MAX_STAKE_PCT, 3))
        execute("UPDATE discipline_rules SET kind = 'max_stake'")

        val error = assertFailsWith<IllegalStateException> { persistence.rules.findById(rule.id) }

        assertEquals("Unknown code 'max_stake' in column discipline_rules.kind", error.message)
    }

    @Test
    fun `data survives reopening the database file`() {
        val stored = SqlitePersistence(path).bankrolls.add(bankroll)

        assertEquals(listOf(stored), SqlitePersistence(path).bankrolls.findAll())
    }
}
