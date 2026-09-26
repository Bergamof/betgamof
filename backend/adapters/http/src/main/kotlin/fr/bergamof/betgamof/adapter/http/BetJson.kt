package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.model.BetView
import fr.bergamof.betgamof.application.model.SelectionView
import kotlinx.serialization.Serializable

@Serializable
data class SelectionJson(
    val eventId: String?,
    val eventName: String,
    val sport: String,
    val competition: String,
    val market: String,
    val pick: String,
    val odds: Double,
)

fun SelectionView.toJson() =
    SelectionJson(
        eventId = eventId,
        eventName = eventName,
        sport = sport,
        competition = competition,
        market = market,
        pick = pick,
        odds = odds,
    )

@Serializable
data class BetJson(
    val id: Long,
    val bankrollId: Long,
    val bankrollName: String,
    val montanteId: Long?,
    val montanteName: String?,
    val palierNumber: Int?,
    val type: BetTypeJson,
    val bookmaker: String,
    val stake: Double,
    val odds: Double,
    val status: BetStatusJson,
    val cashout: Double?,
    val profit: Double,
    val potentialReturn: Double,
    val label: String,
    val sport: String,
    val competition: String,
    val market: String,
    val selections: List<SelectionJson>,
    val placedAt: JsonInstant,
    val startsAt: JsonInstant,
    val settledAt: JsonInstant?,
)

fun BetView.toJson() =
    BetJson(
        id = id.value,
        bankrollId = bankrollId.value,
        bankrollName = bankrollName,
        montanteId = montanteId?.value,
        montanteName = montanteName,
        palierNumber = palierNumber,
        type = type.toJson(),
        bookmaker = bookmaker,
        stake = stake.toEuros(),
        odds = odds,
        status = status.toJson(),
        cashout = cashout?.toEuros(),
        profit = profit.toEuros(),
        potentialReturn = potentialReturn.toEuros(),
        label = betLabel(type, selections),
        sport = sport,
        competition = competitionLabel(selections),
        market = marketLabel(type, selections),
        selections = selections.map { it.toJson() },
        placedAt = placedAt,
        startsAt = startsAt,
        settledAt = settledAt,
    )
