package fr.bergamof.betgamof.inbound.rest

import fr.bergamof.betgamof.business.domain.BankrollColor
import fr.bergamof.betgamof.business.domain.DefaultStake
import fr.bergamof.betgamof.business.domain.Money
import fr.bergamof.betgamof.business.model.BankrollView
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
    val defaultStake: StakeJson?,
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
        defaultStake = defaultStake?.toJson(),
        outsideMontantes = outsideMontantes.toEuros(),
        openStake = openStake.toEuros(),
        staked = staked.toEuros(),
        profit = profit.toEuros(),
        roi = roi,
        betCount = betCount,
        bookmakers = bookmakers,
    )

enum class StakeUnit { EUR, PERCENT }

/** A stake in euros, or in percent of the bankroll balance. */
@Serializable
data class StakeJson(
    val unit: StakeUnit,
    val value: Double,
) {
    fun toDefaultStake(): DefaultStake =
        when (unit) {
            StakeUnit.EUR -> DefaultStake.Amount(Money.euros(value))
            StakeUnit.PERCENT -> DefaultStake.Percent(value)
        }
}

fun DefaultStake.toJson() =
    when (this) {
        is DefaultStake.Amount -> StakeJson(StakeUnit.EUR, amount.toEuros())
        is DefaultStake.Percent -> StakeJson(StakeUnit.PERCENT, percent)
    }
