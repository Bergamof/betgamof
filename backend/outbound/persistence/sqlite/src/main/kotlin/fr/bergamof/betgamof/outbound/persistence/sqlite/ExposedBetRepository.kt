package fr.bergamof.betgamof.outbound.persistence.sqlite

import fr.bergamof.betgamof.business.domain.Bet
import fr.bergamof.betgamof.business.domain.BetChange
import fr.bergamof.betgamof.business.domain.BetStatus
import fr.bergamof.betgamof.business.domain.BetTotals
import fr.bergamof.betgamof.business.domain.BetType
import fr.bergamof.betgamof.business.domain.Money
import fr.bergamof.betgamof.business.domain.NewBet
import fr.bergamof.betgamof.business.domain.Selection
import fr.bergamof.betgamof.business.domain.Settlement
import fr.bergamof.betgamof.business.port.outbound.BetCriteria
import fr.bergamof.betgamof.business.port.outbound.BetRepository
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

/**
 * Bets are filtered, sorted and paged by SQLite ([BetCriteriaQuery]); only the matching bets and their
 * selections are loaded. Totals are folded row by row, so they never hold the bets in memory.
 */
internal class ExposedBetRepository(
    private val db: Database,
) : BetRepository {
    override fun find(id: Long): Bet? = find(BetsTable.id eq id).singleOrNull()

    override fun find(criteria: BetCriteria): List<Bet> = find(BetCriteriaQuery.where(criteria), criteria)

    override fun count(criteria: BetCriteria): Int =
        transaction(db) {
            BetsTable
                .selectAll()
                .where(BetCriteriaQuery.where(criteria))
                .count()
                .toInt()
        }

    override fun directTotalsByBankroll(): Map<Long, BetTotals> =
        transaction(db) {
            val totals = mutableMapOf<Long, BetTotals>()
            BetsTable
                .select(BetsTable.bankrollId, BetsTable.status, BetsTable.stakeCents, BetsTable.odds, BetsTable.cashoutCents)
                .where { BetsTable.montanteId.isNull() }
                .forEach { row ->
                    val bankrollId = row[BetsTable.bankrollId].value
                    totals[bankrollId] =
                        (totals[bankrollId] ?: BetTotals.ZERO).add(
                            status = BetStatus.valueOf(row[BetsTable.status]),
                            stake = Money(row[BetsTable.stakeCents]),
                            odds = row[BetsTable.odds],
                            cashout = row[BetsTable.cashoutCents]?.let(::Money),
                        )
                }
            totals
        }

    override fun bookmakersByUsage(bankrollId: Long): List<String> =
        transaction(db) {
            val uses = BetsTable.id.count()
            BetsTable
                .select(BetsTable.bookmaker, uses)
                .where { BetsTable.bankrollId eq bankrollId }
                .groupBy(BetsTable.bookmaker)
                .orderBy(uses to SortOrder.DESC, BetsTable.bookmaker to SortOrder.ASC)
                .map { it[BetsTable.bookmaker] }
        }

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

    override fun update(
        id: Long,
        change: BetChange,
    ): Boolean =
        transaction(db) {
            BetsTable.update({ BetsTable.id eq id }) {
                it[stakeCents] = change.stake.cents
                it[odds] = change.odds
                it[bookmaker] = change.bookmaker.trim()
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

    /** Matching bets in the criteria order, with their selections loaded by a single second query. */
    private fun find(
        where: Op<Boolean>,
        criteria: BetCriteria = BetCriteria(),
    ): List<Bet> =
        transaction(db) {
            val rows = BetCriteriaQuery.sortedAndPaged(BetsTable.selectAll().where(where), criteria).toList()
            if (rows.isEmpty()) return@transaction emptyList()
            // A page lists its few ids; a full result reuses the filter rather than binding every id.
            val ofTheseBets =
                if (criteria.page == null) {
                    BetSelectionsTable.betId inSubQuery BetsTable.select(BetsTable.id).where(where)
                } else {
                    BetSelectionsTable.betId inList rows.map { it[BetsTable.id] }
                }
            val selections =
                BetSelectionsTable
                    .selectAll()
                    .where(ofTheseBets)
                    .orderBy(BetSelectionsTable.position)
                    .groupBy({ it[BetSelectionsTable.betId].value }, ::toSelection)
            rows.map { toBet(it, selections[it[BetsTable.id].value].orEmpty()) }
        }
}

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
