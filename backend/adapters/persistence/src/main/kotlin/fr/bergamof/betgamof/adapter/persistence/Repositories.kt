package fr.bergamof.betgamof.adapter.persistence

import fr.bergamof.betgamof.application.port.outbound.BankrollRepository
import fr.bergamof.betgamof.application.port.outbound.BetRepository
import fr.bergamof.betgamof.application.port.outbound.MontanteRepository
import fr.bergamof.betgamof.application.port.outbound.RuleRepository
import fr.bergamof.betgamof.domain.Bankroll
import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BankrollSettings
import fr.bergamof.betgamof.domain.Bet
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.DisciplineRule
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.Montante
import fr.bergamof.betgamof.domain.MontanteConfig
import fr.bergamof.betgamof.domain.MontanteMode
import fr.bergamof.betgamof.domain.NewBet
import fr.bergamof.betgamof.domain.RuleKind
import fr.bergamof.betgamof.domain.Selection
import fr.bergamof.betgamof.domain.Settlement
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

// Exposed/SQLite implementations of the application's repository ports.
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
        row[fixedStakeCents] = settings.fixedStake?.cents
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
                    fixedStake = row[BankrollsTable.fixedStakeCents]?.let(::Money),
                ),
            createdAt = Instant.parse(row[BankrollsTable.createdAt]),
        )
}

internal class ExposedBetRepository(
    private val db: Database,
) : BetRepository {
    override fun findAll(): List<Bet> =
        transaction(db) {
            val selections =
                BetSelectionsTable
                    .selectAll()
                    .orderBy(BetSelectionsTable.position)
                    .groupBy({ it[BetSelectionsTable.betId].value }, ::toSelection)
            BetsTable.selectAll().map { toBet(it, selections[it[BetsTable.id].value].orEmpty()) }
        }

    override fun find(id: Long): Bet? = findAll().firstOrNull { it.id == id }

    override fun create(
        bet: NewBet,
        placedAt: Instant,
    ): Long =
        transaction(db) {
            val betId =
                BetsTable
                    .insertAndGetId {
                        it[bankrollId] = bet.bankrollId
                        it[montanteId] = bet.montanteId
                        it[type] = bet.type.name
                        it[bookmaker] = bet.bookmaker.trim()
                        it[stakeCents] = bet.stake.cents
                        it[odds] = bet.odds
                        it[status] = BetStatus.OPEN.name
                        it[this.placedAt] = placedAt.toString()
                        it[startsAt] = bet.startsAt.toString()
                    }.value
            bet.selections.forEachIndexed { index, selection ->
                BetSelectionsTable.insert {
                    it[this.betId] = betId
                    it[position] = index
                    it[eventId] = selection.eventId
                    it[eventName] = selection.eventName.trim()
                    it[sport] = selection.sport
                    it[competition] = selection.competition
                    it[market] = selection.market
                    it[pick] = selection.pick.trim()
                    it[odds] = selection.odds
                }
            }
            betId
        }

    override fun updateStakeAndOdds(
        id: Long,
        stake: Money,
        odds: Double,
        bookmaker: String,
    ): Boolean =
        transaction(db) {
            BetsTable.update({ BetsTable.id eq id }) {
                it[stakeCents] = stake.cents
                it[this.odds] = odds
                it[this.bookmaker] = bookmaker.trim()
            } > 0
        }

    override fun settle(
        id: Long,
        settlement: Settlement,
        at: Instant,
    ): Boolean =
        transaction(db) {
            BetsTable.update({ BetsTable.id eq id }) {
                it[status] = settlement.status.name
                it[cashoutCents] = settlement.cashout?.cents
                it[settledAt] = at.toString()
            } > 0
        }

    override fun delete(id: Long): Boolean = transaction(db) { BetsTable.deleteWhere { BetsTable.id eq id } > 0 }

    private fun toSelection(row: ResultRow) =
        Selection(
            eventId = row[BetSelectionsTable.eventId],
            eventName = row[BetSelectionsTable.eventName],
            sport = row[BetSelectionsTable.sport],
            competition = row[BetSelectionsTable.competition],
            market = row[BetSelectionsTable.market],
            pick = row[BetSelectionsTable.pick],
            odds = row[BetSelectionsTable.odds],
        )

    private fun toBet(
        row: ResultRow,
        selections: List<Selection>,
    ) = Bet(
        id = row[BetsTable.id].value,
        bankrollId = row[BetsTable.bankrollId].value,
        montanteId = row[BetsTable.montanteId]?.value,
        type = BetType.valueOf(row[BetsTable.type]),
        bookmaker = row[BetsTable.bookmaker],
        stake = Money(row[BetsTable.stakeCents]),
        odds = row[BetsTable.odds],
        status = BetStatus.valueOf(row[BetsTable.status]),
        cashout = row[BetsTable.cashoutCents]?.let(::Money),
        selections = selections,
        placedAt = Instant.parse(row[BetsTable.placedAt]),
        startsAt = Instant.parse(row[BetsTable.startsAt]),
        settledAt = row[BetsTable.settledAt]?.let(Instant::parse),
    )
}

internal class ExposedMontanteRepository(
    private val db: Database,
) : MontanteRepository {
    override fun findAll(): List<Montante> = transaction(db) { MontantesTable.selectAll().orderBy(MontantesTable.id).map(::toMontante) }

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

    override fun create(
        kind: RuleKind,
        param: Int,
    ): Long =
        transaction(db) {
            DisciplineRulesTable
                .insertAndGetId {
                    it[this.kind] = kind.name
                    it[this.param] = param
                }.value
        }

    override fun delete(id: Long): Boolean = transaction(db) { DisciplineRulesTable.deleteWhere { DisciplineRulesTable.id eq id } > 0 }
}
