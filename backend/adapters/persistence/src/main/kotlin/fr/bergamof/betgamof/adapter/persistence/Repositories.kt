package fr.bergamof.betgamof.adapter.persistence

import fr.bergamof.betgamof.application.port.outbound.BankrollRepository
import fr.bergamof.betgamof.application.port.outbound.BetRepository
import fr.bergamof.betgamof.application.port.outbound.MontanteRepository
import fr.bergamof.betgamof.application.port.outbound.RuleRepository
import fr.bergamof.betgamof.domain.Bankroll
import fr.bergamof.betgamof.domain.BankrollId
import fr.bergamof.betgamof.domain.BankrollSettings
import fr.bergamof.betgamof.domain.Bet
import fr.bergamof.betgamof.domain.BetId
import fr.bergamof.betgamof.domain.DisciplineRule
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.Montante
import fr.bergamof.betgamof.domain.MontanteConfig
import fr.bergamof.betgamof.domain.MontanteId
import fr.bergamof.betgamof.domain.RuleId
import fr.bergamof.betgamof.domain.Selection
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

// Exposed/SQLite implementations of the application's repository ports.
// They only map aggregates to rows and back: validation, normalisation and state changes belong to the domain,
// and stored values were normalised by the domain before being written, so they are read back as they are.
// Every method runs through the transaction runner, so it joins the use case's transaction when there is one.

internal class ExposedBankrollRepository(
    private val tx: SqliteTransactionRunner,
) : BankrollRepository {
    override fun findAll(): List<Bankroll> = tx.execute { BankrollsTable.selectAll().orderBy(BankrollsTable.id).map(::toBankroll) }

    override fun findById(id: BankrollId): Bankroll? =
        tx.execute {
            BankrollsTable
                .selectAll()
                .where { BankrollsTable.id eq id.value }
                .singleOrNull()
                ?.let(::toBankroll)
        }

    override fun add(bankroll: Bankroll): Bankroll =
        tx.execute {
            val id = BankrollsTable.insertAndGetId { write(it, bankroll) }.value
            bankroll.copy(id = BankrollId(id))
        }

    override fun save(bankroll: Bankroll) {
        tx.execute {
            val updated = BankrollsTable.update({ BankrollsTable.id eq bankroll.id.value }) { write(it, bankroll) }
            check(updated == 1) { "Bankroll ${bankroll.id.value} does not exist" }
        }
    }

    override fun remove(id: BankrollId) {
        tx.execute { BankrollsTable.deleteWhere { BankrollsTable.id eq id.value } }
    }

    private fun BankrollsTable.write(
        row: UpdateBuilder<*>,
        bankroll: Bankroll,
    ) {
        val settings = bankroll.settings
        row[name] = settings.name
        row[color] = bankrollColorCodes.encode(settings.color)
        row[initialBalanceCents] = settings.initialBalance.cents
        row[stopLossCents] = settings.stopLoss?.cents
        row[kellyFraction] = settings.kellyFraction
        row[fixedStakeCents] = settings.fixedStake?.cents
        row[createdAt] = bankroll.createdAt.toString()
    }

    private fun toBankroll(row: ResultRow) =
        Bankroll(
            id = BankrollId(row[BankrollsTable.id].value),
            settings =
                BankrollSettings.of(
                    name = row[BankrollsTable.name],
                    color = bankrollColorCodes.decode(row[BankrollsTable.color]),
                    initialBalance = Money(row[BankrollsTable.initialBalanceCents]),
                    stopLoss = row[BankrollsTable.stopLossCents]?.let(::Money),
                    kellyFraction = row[BankrollsTable.kellyFraction],
                    fixedStake = row[BankrollsTable.fixedStakeCents]?.let(::Money),
                ),
            createdAt = Instant.parse(row[BankrollsTable.createdAt]),
        )
}

internal class ExposedBetRepository(
    private val tx: SqliteTransactionRunner,
) : BetRepository {
    override fun findAll(): List<Bet> =
        tx.execute {
            val selections =
                BetSelectionsTable
                    .selectAll()
                    .orderBy(BetSelectionsTable.betId to SortOrder.ASC, BetSelectionsTable.position to SortOrder.ASC)
                    .groupBy({ it[BetSelectionsTable.betId].value }, ::toSelection)
            BetsTable.selectAll().orderBy(BetsTable.id).map { toBet(it, selections[it[BetsTable.id].value].orEmpty()) }
        }

    /** Loads the bet row and its own selections only. */
    override fun findById(id: BetId): Bet? =
        tx.execute {
            BetsTable
                .selectAll()
                .where { BetsTable.id eq id.value }
                .singleOrNull()
                ?.let { toBet(it, selectionsOf(id)) }
        }

    override fun add(bet: Bet): Bet =
        tx.execute {
            val id = BetId(BetsTable.insertAndGetId { write(it, bet) }.value)
            insertSelections(id, bet.selections)
            bet.copy(id = id)
        }

    /** Rewrites the bet row and replaces its selections. */
    override fun save(bet: Bet) {
        tx.execute {
            val updated = BetsTable.update({ BetsTable.id eq bet.id.value }) { write(it, bet) }
            check(updated == 1) { "Bet ${bet.id.value} does not exist" }
            BetSelectionsTable.deleteWhere { BetSelectionsTable.betId eq bet.id.value }
            insertSelections(bet.id, bet.selections)
        }
    }

    /** The selections go with their bet (ON DELETE CASCADE). */
    override fun remove(id: BetId) {
        tx.execute { BetsTable.deleteWhere { BetsTable.id eq id.value } }
    }

    private fun BetsTable.write(
        row: UpdateBuilder<*>,
        bet: Bet,
    ) {
        row[bankrollId] = bet.bankrollId.value
        row[montanteId] = bet.montanteId?.value
        row[type] = betTypeCodes.encode(bet.type)
        row[bookmaker] = bet.bookmaker
        row[stakeCents] = bet.stake.cents
        row[odds] = bet.odds
        row[status] = betStatusCodes.encode(bet.status)
        row[cashoutCents] = bet.cashout?.cents
        row[placedAt] = bet.placedAt.toString()
        row[startsAt] = bet.startsAt.toString()
        row[settledAt] = bet.settledAt?.toString()
    }

    private fun insertSelections(
        betId: BetId,
        selections: List<Selection>,
    ) {
        BetSelectionsTable.batchInsert(selections.withIndex(), shouldReturnGeneratedValues = false) { (index, selection) ->
            this[BetSelectionsTable.betId] = betId.value
            this[BetSelectionsTable.position] = index
            this[BetSelectionsTable.eventId] = selection.eventId
            this[BetSelectionsTable.eventName] = selection.eventName
            this[BetSelectionsTable.sport] = selection.sport
            this[BetSelectionsTable.competition] = selection.competition
            this[BetSelectionsTable.market] = selection.market
            this[BetSelectionsTable.pick] = selection.pick
            this[BetSelectionsTable.odds] = selection.odds
        }
    }

    private fun selectionsOf(betId: BetId) =
        BetSelectionsTable
            .selectAll()
            .where { BetSelectionsTable.betId eq betId.value }
            .orderBy(BetSelectionsTable.position)
            .map(::toSelection)

    private fun toSelection(row: ResultRow) =
        Selection.of(
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
        id = BetId(row[BetsTable.id].value),
        bankrollId = BankrollId(row[BetsTable.bankrollId].value),
        montanteId = row[BetsTable.montanteId]?.value?.let(::MontanteId),
        type = betTypeCodes.decode(row[BetsTable.type]),
        bookmaker = row[BetsTable.bookmaker],
        stake = Money(row[BetsTable.stakeCents]),
        odds = row[BetsTable.odds],
        status = betStatusCodes.decode(row[BetsTable.status]),
        cashout = row[BetsTable.cashoutCents]?.let(::Money),
        selections = selections,
        placedAt = Instant.parse(row[BetsTable.placedAt]),
        startsAt = Instant.parse(row[BetsTable.startsAt]),
        settledAt = row[BetsTable.settledAt]?.let(Instant::parse),
    )
}

internal class ExposedMontanteRepository(
    private val tx: SqliteTransactionRunner,
) : MontanteRepository {
    override fun findAll(): List<Montante> = tx.execute { MontantesTable.selectAll().orderBy(MontantesTable.id).map(::toMontante) }

    override fun findById(id: MontanteId): Montante? =
        tx.execute {
            MontantesTable
                .selectAll()
                .where { MontantesTable.id eq id.value }
                .singleOrNull()
                ?.let(::toMontante)
        }

    override fun add(montante: Montante): Montante =
        tx.execute {
            val id = MontantesTable.insertAndGetId { write(it, montante) }.value
            montante.copy(id = MontanteId(id))
        }

    override fun save(montante: Montante) {
        tx.execute {
            val updated = MontantesTable.update({ MontantesTable.id eq montante.id.value }) { write(it, montante) }
            check(updated == 1) { "Montante ${montante.id.value} does not exist" }
        }
    }

    private fun MontantesTable.write(
        row: UpdateBuilder<*>,
        montante: Montante,
    ) {
        val config = montante.config
        row[name] = config.name
        row[bankrollId] = config.bankrollId.value
        row[startCapitalCents] = config.startCapital.cents
        row[targetOdds] = config.targetOdds
        row[mode] = montanteModeCodes.encode(config.mode)
        row[targetMultiplier] = config.targetMultiplier
        row[stepCount] = config.stepCount
        row[excludeStake] = config.excludeStake
        row[securePct] = config.securePct
        row[relancesAllowed] = config.relancesAllowed
        row[createdAt] = montante.createdAt.toString()
        row[closedAt] = montante.closedAt?.toString()
    }

    private fun toMontante(row: ResultRow) =
        Montante(
            id = MontanteId(row[MontantesTable.id].value),
            config =
                MontanteConfig.of(
                    name = row[MontantesTable.name],
                    bankrollId = BankrollId(row[MontantesTable.bankrollId].value),
                    startCapital = Money(row[MontantesTable.startCapitalCents]),
                    targetOdds = row[MontantesTable.targetOdds],
                    mode = montanteModeCodes.decode(row[MontantesTable.mode]),
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
    private val tx: SqliteTransactionRunner,
) : RuleRepository {
    override fun findAll(): List<DisciplineRule> =
        tx.execute { DisciplineRulesTable.selectAll().orderBy(DisciplineRulesTable.id).map(::toRule) }

    override fun findById(id: RuleId): DisciplineRule? =
        tx.execute {
            DisciplineRulesTable
                .selectAll()
                .where { DisciplineRulesTable.id eq id.value }
                .singleOrNull()
                ?.let(::toRule)
        }

    override fun add(rule: DisciplineRule): DisciplineRule =
        tx.execute {
            val id =
                DisciplineRulesTable
                    .insertAndGetId {
                        it[kind] = ruleKindCodes.encode(rule.kind)
                        it[param] = rule.param
                    }.value
            rule.copy(id = RuleId(id))
        }

    override fun remove(id: RuleId) {
        tx.execute { DisciplineRulesTable.deleteWhere { DisciplineRulesTable.id eq id.value } }
    }

    private fun toRule(row: ResultRow) =
        DisciplineRule(
            id = RuleId(row[DisciplineRulesTable.id].value),
            kind = ruleKindCodes.decode(row[DisciplineRulesTable.kind]),
            param = row[DisciplineRulesTable.param],
        )
}
