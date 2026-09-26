package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.model.SelectionView
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.OddsRange
import fr.bergamof.betgamof.domain.SegmentKey

// Display strings of the JSON contract, computed from the structured read models.

private const val SEPARATOR = " · "

/** Market label shared by combined and system bets, which span several markets. */
private const val MULTIPLE_MARKETS = "Combiné"

/** "Event · pick" for a single bet, otherwise the kind of bet and its number of selections. */
internal fun betLabel(
    type: BetType,
    selections: List<SelectionView>,
): String =
    when (type) {
        BetType.SIMPLE -> selections.first().let { "${it.eventName}$SEPARATOR${it.pick}" }
        BetType.COMBINE -> "Combiné ${selections.size} sélections"
        BetType.SYSTEME -> "Système ${selections.size} sélections"
    }

internal fun competitionLabel(selections: List<SelectionView>): String =
    selections.map { it.competition }.distinct().joinToString(SEPARATOR)

internal fun marketLabel(
    type: BetType,
    selections: List<SelectionView>,
): String = if (type == BetType.SIMPLE) selections.first().market else MULTIPLE_MARKETS

internal fun SegmentKey.label(): String =
    when (this) {
        is SegmentKey.Sport -> name
        is SegmentKey.Market -> name
        SegmentKey.MultipleMarkets -> MULTIPLE_MARKETS
        is SegmentKey.Odds -> range.label()
        SegmentKey.LateNight -> "late-night"
        SegmentKey.DayBefore -> "day-before"
    }

internal fun OddsRange.label(): String =
    when (this) {
        OddsRange.SAFE -> "1.0 – 1.5"
        OddsRange.MEDIUM -> "1.5 – 2.0"
        OddsRange.VALUE -> "2.0 – 3.0"
        OddsRange.LONG_SHOT -> "3.0 +"
    }
