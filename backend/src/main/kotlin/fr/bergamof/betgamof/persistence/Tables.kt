package fr.bergamof.betgamof.persistence

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

// Schema is owned by Flyway (src/main/resources/db/migration); these objects only map it.
// Instants are stored as ISO-8601 text, which sorts chronologically in SQLite.

object BankrollsTable : LongIdTable("bankrolls") {
    val name = text("name")
    val color = text("color")
    val initialBalanceCents = long("initial_balance_cents")
    val stopLossCents = long("stop_loss_cents").nullable()
    val kellyFraction = double("kelly_fraction")
    val fixedStakeCents = long("fixed_stake_cents").nullable()
    val createdAt = text("created_at")
}

object MontantesTable : LongIdTable("montantes") {
    val name = text("name")
    val bankrollId = reference("bankroll_id", BankrollsTable)
    val startCapitalCents = long("start_capital_cents")
    val targetOdds = double("target_odds")
    val mode = text("mode")
    val targetMultiplier = double("target_multiplier").nullable()
    val stepCount = integer("step_count").nullable()
    val excludeStake = bool("exclude_stake")
    val securePct = integer("secure_pct")
    val relancesAllowed = integer("relances_allowed")
    val createdAt = text("created_at")
    val closedAt = text("closed_at").nullable()
}

object BetsTable : LongIdTable("bets") {
    val bankrollId = reference("bankroll_id", BankrollsTable)
    val montanteId = reference("montante_id", MontantesTable).nullable()
    val type = text("type")
    val bookmaker = text("bookmaker")
    val stakeCents = long("stake_cents")
    val odds = double("odds")
    val status = text("status")
    val cashoutCents = long("cashout_cents").nullable()
    val placedAt = text("placed_at")
    val startsAt = text("starts_at")
    val settledAt = text("settled_at").nullable()
}

object BetSelectionsTable : LongIdTable("bet_selections") {
    val betId = reference("bet_id", BetsTable, onDelete = ReferenceOption.CASCADE)
    val position = integer("position")
    val eventId = text("event_id").nullable()
    val eventName = text("event_name")
    val sport = text("sport")
    val competition = text("competition")
    val market = text("market")
    val pick = text("pick")
    val odds = double("odds")
}

object DisciplineRulesTable : LongIdTable("discipline_rules") {
    val kind = text("kind")
    val param = integer("param")
}
