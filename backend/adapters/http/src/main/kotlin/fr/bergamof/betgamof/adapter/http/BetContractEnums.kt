package fr.bergamof.betgamof.adapter.http

import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.MarketCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Contract enums of bets and events; see ContractEnums.kt.

@Serializable
@SerialName("BetType")
enum class BetTypeJson { SIMPLE, COMBINE, SYSTEME }

@Serializable
@SerialName("BetStatus")
enum class BetStatusJson { OPEN, WON, LOST, VOID, CASHOUT }

@Serializable
@SerialName("MarketCategory")
enum class MarketCategoryJson { RESULTAT, BUTS, HANDICAP, BUTEURS, MI_TEMPS }

fun BetType.toJson() =
    when (this) {
        BetType.SIMPLE -> BetTypeJson.SIMPLE
        BetType.COMBINE -> BetTypeJson.COMBINE
        BetType.SYSTEME -> BetTypeJson.SYSTEME
    }

fun BetTypeJson.toDomain() =
    when (this) {
        BetTypeJson.SIMPLE -> BetType.SIMPLE
        BetTypeJson.COMBINE -> BetType.COMBINE
        BetTypeJson.SYSTEME -> BetType.SYSTEME
    }

fun BetStatus.toJson() =
    when (this) {
        BetStatus.OPEN -> BetStatusJson.OPEN
        BetStatus.WON -> BetStatusJson.WON
        BetStatus.LOST -> BetStatusJson.LOST
        BetStatus.VOID -> BetStatusJson.VOID
        BetStatus.CASHOUT -> BetStatusJson.CASHOUT
    }

fun BetStatusJson.toDomain() =
    when (this) {
        BetStatusJson.OPEN -> BetStatus.OPEN
        BetStatusJson.WON -> BetStatus.WON
        BetStatusJson.LOST -> BetStatus.LOST
        BetStatusJson.VOID -> BetStatus.VOID
        BetStatusJson.CASHOUT -> BetStatus.CASHOUT
    }

/** Response only: clients never send a market category. */
fun MarketCategory.toJson() =
    when (this) {
        MarketCategory.RESULTAT -> MarketCategoryJson.RESULTAT
        MarketCategory.BUTS -> MarketCategoryJson.BUTS
        MarketCategory.HANDICAP -> MarketCategoryJson.HANDICAP
        MarketCategory.BUTEURS -> MarketCategoryJson.BUTEURS
        MarketCategory.MI_TEMPS -> MarketCategoryJson.MI_TEMPS
    }
