package fr.bergamof.betgamof.outbound.persistence.sqlite

import fr.bergamof.betgamof.business.domain.Bankroll
import fr.bergamof.betgamof.business.domain.BankrollColor
import fr.bergamof.betgamof.business.domain.BankrollSettings
import fr.bergamof.betgamof.business.domain.DefaultStake
import fr.bergamof.betgamof.business.domain.DisciplineRule
import fr.bergamof.betgamof.business.domain.Money
import fr.bergamof.betgamof.business.domain.Montante
import fr.bergamof.betgamof.business.domain.MontanteConfig
import fr.bergamof.betgamof.business.domain.MontanteMode
import fr.bergamof.betgamof.business.domain.NewRule
import fr.bergamof.betgamof.business.domain.RuleKind
import fr.bergamof.betgamof.business.port.outbound.BankrollRepository
import fr.bergamof.betgamof.business.port.outbound.MontanteRepository
import fr.bergamof.betgamof.business.port.outbound.RuleRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

// Exposed/SQLite implementations of the business repository ports.
// A personal betting log stays small (thousands of rows at most): repositories load whole
// aggregates and let the domain filter in memory, which keeps every calculation in one place.

internal class ExposedBankrollRepository(
    private val db: Database,
) : BankrollRepository {
    override fun findAll(): List<Bankroll> = transaction(db) { BankrollsTable.selectAll().orderBy(BankrollsTable.id).map(::toBankroll) }

    override fun find(id: Long): Bankroll? =
        transaction(db) {
            BankrollsTable
                .selectAll()
                .where { BankrollsTable.id eq id }
                .singleOrNull()
                ?.let(::toBankroll)
        }

    override fun create(
        settings: BankrollSettings,
        now: Instant,
    ): Long =
        transaction(db) {
            BankrollsTable
                .insertAndGetId {
                    write(it, settings)
                    it[createdAt] = now.toString()
                }.value
        }

    override fun update(
        id: Long,
        settings: BankrollSettings,
    ): Boolean = transaction(db) { BankrollsTable.update({ BankrollsTable.id eq id }) { write(it, settings) } > 0 }

    override fun delete(id: Long): Boolean = transaction(db) { BankrollsTable.deleteWhere { BankrollsTable.id eq id } > 0 }

    private fun BankrollsTable.write(
        row: UpdateBuilder<*>,
        settings: BankrollSettings,
    ) {
        row[name] = settings.name.trim()
        row[color] = settings.color.name
        row[initialBalanceCents] = settings.initialBalance.cents
        row[stopLossCents] = settings.stopLoss?.cents
        row[kellyFraction] = settings.kellyFraction
        val defaultStake = settings.defaultStake
        row[defaultStakeCents] = (defaultStake as? DefaultStake.Amount)?.amount?.cents
        row[defaultStakePercent] = (defaultStake as? DefaultStake.Percent)?.percent
    }

    private fun toBankroll(row: ResultRow) =
        Bankroll(
            id = row[BankrollsTable.id].value,
            settings =
                BankrollSettings(
                    name = row[BankrollsTable.name],
                    color = BankrollColor.valueOf(row[BankrollsTable.color]),
                    initialBalance = Money(row[BankrollsTable.initialBalanceCents]),
                    stopLoss = row[BankrollsTable.stopLossCents]?.let(::Money),
                    kellyFraction = row[BankrollsTable.kellyFraction],
                    defaultStake = toDefaultStake(row),
                ),
            createdAt = Instant.parse(row[BankrollsTable.createdAt]),
        )

    private fun toDefaultStake(row: ResultRow): DefaultStake? =
        row[BankrollsTable.defaultStakeCents]?.let { DefaultStake.Amount(Money(it)) }
            ?: row[BankrollsTable.defaultStakePercent]?.let(DefaultStake::Percent)
}

internal class ExposedMontanteRepository(
    private val db: Database,
) : MontanteRepository {
    override fun findAll(): List<Montante> = transaction(db) { MontantesTable.selectAll().orderBy(MontantesTable.id).map(::toMontante) }

    override fun findByBankroll(bankrollId: Long): List<Montante> =
        transaction(db) {
            MontantesTable
                .selectAll()
                .where { MontantesTable.bankrollId eq bankrollId }
                .orderBy(MontantesTable.id)
                .map(::toMontante)
        }

    override fun find(id: Long): Montante? =
        transaction(db) {
            MontantesTable
                .selectAll()
                .where { MontantesTable.id eq id }
                .singleOrNull()
                ?.let(::toMontante)
        }

    override fun create(
        config: MontanteConfig,
        now: Instant,
    ): Long =
        transaction(db) {
            MontantesTable
                .insertAndGetId {
                    it[name] = config.name.trim()
                    it[bankrollId] = config.bankrollId
                    it[startCapitalCents] = config.startCapital.cents
                    it[targetOdds] = config.targetOdds
                    it[mode] = config.mode.name
                    it[targetMultiplier] = config.targetMultiplier
                    it[stepCount] = config.stepCount
                    it[excludeStake] = config.excludeStake
                    it[securePct] = config.securePct
                    it[relancesAllowed] = config.relancesAllowed
                    it[createdAt] = now.toString()
                }.value
        }

    override fun close(
        id: Long,
        at: Instant,
    ): Boolean = transaction(db) { MontantesTable.update({ MontantesTable.id eq id }) { it[closedAt] = at.toString() } > 0 }

    private fun toMontante(row: ResultRow) =
        Montante(
            id = row[MontantesTable.id].value,
            config =
                MontanteConfig(
                    name = row[MontantesTable.name],
                    bankrollId = row[MontantesTable.bankrollId].value,
                    startCapital = Money(row[MontantesTable.startCapitalCents]),
                    targetOdds = row[MontantesTable.targetOdds],
                    mode = MontanteMode.valueOf(row[MontantesTable.mode]),
                    targetMultiplier = row[MontantesTable.targetMultiplier],
                    stepCount = row[MontantesTable.stepCount],
                    excludeStake = row[MontantesTable.excludeStake],
                    securePct = row[MontantesTable.securePct],
                    relancesAllowed = row[MontantesTable.relancesAllowed],
                ),
            createdAt = Instant.parse(row[MontantesTable.createdAt]),
            closedAt = row[MontantesTable.closedAt]?.let(Instant::parse),
        )
}

internal class ExposedRuleRepository(
    private val db: Database,
) : RuleRepository {
    override fun findAll(): List<DisciplineRule> =
        transaction(db) {
            DisciplineRulesTable.selectAll().orderBy(DisciplineRulesTable.id).map {
                DisciplineRule(
                    it[DisciplineRulesTable.id].value,
                    RuleKind.valueOf(it[DisciplineRulesTable.kind]),
                    it[DisciplineRulesTable.param],
                )
            }
        }

    override fun create(rule: NewRule): Long =
        transaction(db) {
            DisciplineRulesTable
                .insertAndGetId {
                    it[kind] = rule.kind.name
                    it[param] = rule.param
                }.value
        }

    override fun delete(id: Long): Boolean = transaction(db) { DisciplineRulesTable.deleteWhere { DisciplineRulesTable.id eq id } > 0 }
}
