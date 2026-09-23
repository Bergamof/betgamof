package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.application.model.BankrollView
import fr.bergamof.betgamof.domain.BankrollColor
import kotlinx.serialization.Serializable

// JSON contract of the REST API: amounts in euros, instants in ISO-8601.
// Mirrored by frontend/src/lib/api/types.ts.

@Serializable
data class BankrollJson(
    val id: Long,
    val name: String,
    val color: BankrollColor,
    val initialBalance: Double,
    val balance: Double,
    val stopLoss: Double?,
    val stopLossMargin: Double?,
    val kellyFraction: Double,
    val fixedStake: Double?,
    val outsideMontantes: Double,
    val openStake: Double,
    val staked: Double,
    val profit: Double,
    val roi: Double,
    val betCount: Int,
    val bookmakers: List<String>,
)

fun BankrollView.toJson() =
    BankrollJson(
        id = id,
        name = name,
        color = color,
        initialBalance = initialBalance.toEuros(),
        balance = balance.toEuros(),
        stopLoss = stopLoss?.toEuros(),
        stopLossMargin = stopLossMargin?.toEuros(),
        kellyFraction = kellyFraction,
        fixedStake = fixedStake?.toEuros(),
        outsideMontantes = outsideMontantes.toEuros(),
        openStake = openStake.toEuros(),
        staked = staked.toEuros(),
        profit = profit.toEuros(),
        roi = roi,
        betCount = betCount,
        bookmakers = bookmakers,
    )
