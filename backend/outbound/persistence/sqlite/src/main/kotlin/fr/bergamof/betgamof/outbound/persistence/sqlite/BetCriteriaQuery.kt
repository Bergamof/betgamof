package fr.bergamof.betgamof.outbound.persistence.sqlite

import fr.bergamof.betgamof.business.domain.BetStatus
import fr.bergamof.betgamof.business.domain.Period
import fr.bergamof.betgamof.business.port.outbound.BetCriteria
import fr.bergamof.betgamof.business.port.outbound.BetOrder
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.DoubleColumnType
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.LikePattern
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exists
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.stringParam
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant

private const val LIKE_ESCAPE = '\\'

/** Translates [BetCriteria] into SQL, following the semantics of [BetCriteria.matches] and [BetOrder.comparator]. */
internal object BetCriteriaQuery {
    fun where(criteria: BetCriteria): Op<Boolean> =
        listOfNotNull(
            criteria.bankrollId?.let { BetsTable.bankrollId eq it },
            criteria.montanteIds?.let { ids -> BetsTable.montanteId inList ids },
            BetsTable.montanteId.isNull().takeIf { criteria.directOnly },
            criteria.status?.let { BetsTable.status eq it.name },
            (BetsTable.status inList listOf(BetStatus.WON.name, BetStatus.LOST.name)).takeIf { criteria.decidedOnly },
            criteria.sport?.let { sport -> firstSelectionSport(sport) },
            criteria.bookmaker?.let { BetsTable.bookmaker.lowerCase() eq it.lowercase() },
            criteria.text?.takeIf { it.isNotBlank() }?.let { containsText(it.trim()) },
            criteria.oddsRange?.let { (BetsTable.odds greaterEq it.min) and (BetsTable.odds less it.max) },
            criteria.placed?.let { within(BetsTable.placedAt, it) },
            criteria.starts?.let { within(BetsTable.startsAt, it) },
            criteria.settled?.let { BetsTable.settledAt.isNotNull() and within(BetsTable.settledAt, it) },
        ).fold(Op.TRUE as Op<Boolean>) { all, condition -> all and condition }

    fun sortedAndPaged(
        query: Query,
        criteria: BetCriteria,
    ): Query {
        val sorted =
            when (criteria.order) {
                BetOrder.CHRONOLOGICAL -> query.orderBy(instant(BetsTable.placedAt) to SortOrder.ASC, BetsTable.id to SortOrder.ASC)
                BetOrder.LATEST_START -> query.orderBy(instant(BetsTable.startsAt) to SortOrder.DESC, BetsTable.id to SortOrder.DESC)
                BetOrder.LATEST_SETTLED ->
                    query.orderBy(instant(BetsTable.settledAt) to SortOrder.DESC_NULLS_LAST, BetsTable.id to SortOrder.DESC)
            }
        return criteria.page?.let { sorted.limit(it.limit).offset(it.offset.toLong()) } ?: sorted
    }

    /**
     * Instants are stored as ISO-8601 text whose fractional part varies in length, so text order is not
     * time order within a second: compare them as Julian days.
     */
    private fun instant(column: Expression<*>) = CustomFunction("julianday", DoubleColumnType(), column)

    private fun within(
        column: Column<*>,
        period: Period,
    ): Op<Boolean> =
        listOfNotNull(
            period.from?.let { instant(column) greaterEq instant(parameter(it)) },
            period.until?.let { instant(column) less instant(parameter(it)) },
        ).fold(Op.TRUE as Op<Boolean>) { all, condition -> all and condition }

    private fun parameter(instant: Instant) = stringParam(instant.toString())

    private fun firstSelectionSport(sport: String) =
        exists(
            BetSelectionsTable
                .selectAll()
                .where {
                    (BetSelectionsTable.betId eq BetsTable.id) and (BetSelectionsTable.position eq 0) and
                        (BetSelectionsTable.sport.lowerCase() eq sport.lowercase())
                },
        )

    /** SQLite `LIKE` ignores the case of ASCII letters only. */
    private fun containsText(text: String): Op<Boolean> {
        val pattern = LikePattern("%" + escape(text) + "%", LIKE_ESCAPE)
        val inSelections =
            exists(
                BetSelectionsTable
                    .selectAll()
                    .where {
                        (BetSelectionsTable.betId eq BetsTable.id) and
                            (
                                (BetSelectionsTable.eventName like pattern) or (BetSelectionsTable.pick like pattern) or
                                    (BetSelectionsTable.competition like pattern) or (BetSelectionsTable.sport like pattern)
                            )
                    },
            )
        return inSelections or (BetsTable.bookmaker like pattern)
    }

    private fun escape(text: String) =
        text
            .replace("$LIKE_ESCAPE", "$LIKE_ESCAPE$LIKE_ESCAPE")
            .replace("%", "$LIKE_ESCAPE%")
            .replace("_", "${LIKE_ESCAPE}_")
}
