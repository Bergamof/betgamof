package fr.bergamof.betgamof.seed

import fr.bergamof.betgamof.domain.BankrollColor
import fr.bergamof.betgamof.domain.BankrollSettings
import fr.bergamof.betgamof.domain.BetStatus
import fr.bergamof.betgamof.domain.BetType
import fr.bergamof.betgamof.domain.Money
import fr.bergamof.betgamof.domain.MontanteConfig
import fr.bergamof.betgamof.domain.MontanteEngine
import fr.bergamof.betgamof.domain.MontanteMode
import fr.bergamof.betgamof.domain.NewBet
import fr.bergamof.betgamof.domain.RuleKind
import fr.bergamof.betgamof.domain.Selection
import fr.bergamof.betgamof.domain.Settlement
import fr.bergamof.betgamof.persistence.BankrollRepository
import fr.bergamof.betgamof.persistence.BetRepository
import fr.bergamof.betgamof.persistence.MontanteRepository
import fr.bergamof.betgamof.persistence.RuleRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random

/**
 * Fills an empty database with the sample data of the design mock-ups (dates relative to today),
 * plus three months of generated history so that statistics have something to show.
 * Enabled with BETGAMOF_SEED_DEMO=true; never touches a database that already has bankrolls.
 */
class DemoSeeder(
    private val bankrolls: BankrollRepository,
    private val bets: BetRepository,
    private val montantes: MontanteRepository,
    private val rules: RuleRepository,
    private val clock: Clock,
    private val zone: ZoneId,
) {
    private val random = Random(SEED)

    fun seedIfEmpty() {
        if (bankrolls.findAll().isNotEmpty()) return
        val start = clock.instant() - Duration.ofDays(HISTORY_DAYS)
        val main =
            bankrolls.create(
                BankrollSettings("Principale", BankrollColor.GAZON, Money.euros(1000), Money.euros(900), 0.25, null),
                start,
            )
        val funBankroll =
            bankrolls.create(
                BankrollSettings("Fun / longs shots", BankrollColor.CIEL, Money.euros(250), null, 0.1, Money.euros(5)),
                start,
            )
        seedHistory(main, funBankroll)
        seedFinishedMontantes(main)
        seedRecentBets(main)
        seedActiveMontante(main)
        rules.create(RuleKind.MAX_STAKE_PCT, MAX_STAKE_PCT)
        rules.create(RuleKind.PAUSE_AFTER_LOSSES, 2)
        rules.create(RuleKind.SINGLE_ACTIVE_MONTANTE, 0)
    }

    private fun at(
        daysAgo: Long,
        time: String,
    ): Instant =
        clock
            .instant()
            .atZone(zone)
            .toLocalDate()
            .minusDays(daysAgo)
            .atTime(LocalTime.parse(time))
            .atZone(zone)
            .toInstant()

    private fun place(
        bankrollId: Long,
        selection: Selection,
        stake: Int,
        bookmaker: String,
        startsAt: Instant,
        result: BetStatus? = null,
        montanteId: Long? = null,
        placedBefore: Duration = Duration.ofHours(3),
    ): Long {
        val bet = NewBet(bankrollId, montanteId, BetType.SIMPLE, bookmaker, Money.euros(stake), selection.odds, startsAt, listOf(selection))
        return record(bet, startsAt - placedBefore, result)
    }

    private fun record(
        bet: NewBet,
        placedAt: Instant,
        result: BetStatus?,
    ): Long {
        val id = bets.create(bet, placedAt)
        result?.let { bets.settle(id, Settlement(it, null), bet.startsAt + Duration.ofHours(2)) }
        return id
    }

    private fun seedRecentBets(main: Long) {
        place(main, football("Lens – Lyon", "Ligue 1", "Total de buts", "Plus de 1,5", 1.72, "lens-lyon"), 25, "Winamax", at(0, "21:00"))
        val combo =
            listOf(
                Selection(null, "ASVEL – Monaco", "Basket", "Euroligue", "Vainqueur", "Monaco", 1.55),
                Selection(null, "Real – Fenerbahçe", "Basket", "Euroligue", "Vainqueur", "Real", 1.50),
                Selection(null, "Olympiakos – Baskonia", "Basket", "Euroligue", "Vainqueur", "Olympiakos", 1.87),
            )
        record(
            NewBet(main, null, BetType.COMBINE, "Unibet", Money.euros(10), COMBO_ODDS, at(0, "18:00"), combo),
            at(0, "12:00"),
            BetStatus.WON,
        )
        place(main, football("PSG – Nice", "Ligue 1", "Résultat final", "PSG gagne", 1.45), 20, "Winamax", at(1, "21:00"), BetStatus.WON)
        place(
            main,
            football("Brest – Lille", "Ligue 1", "Total de buts", "Plus de 2,5", 1.80),
            15,
            "Betclic",
            at(1, "19:00"),
            BetStatus.LOST,
        )
        place(main, tennis("Alcaraz – Sinner", "US Open", "Alcaraz gagne", 2.30), 20, "Winamax", at(2, "22:00"), BetStatus.WON)
    }

    private fun seedActiveMontante(main: Long) {
        val config = MontanteConfig("Montante Ligue 1", main, Money.euros(160), 1.75, MontanteMode.OBJECTIVE, 3.0, null, true, 30, 3)
        val id = montantes.create(config, at(5, "10:00"))
        val paliers =
            listOf(
                Triple(football("Reims – Nantes", "Ligue 1", "Total de buts", "Plus de 1,5", 1.72), "Winamax", BetStatus.WON),
                Triple(football("Milan – Bologne", "Serie A", "Résultat final", "Milan gagne", 1.40), "Betclic", BetStatus.WON),
                Triple(football("Betis – Getafe", "Liga", "Handicap", "Betis -0,5", 1.65), "Unibet", BetStatus.LOST),
                Triple(football("Porto – Braga", "Liga Portugal", "Total de buts", "Plus de 1,5", 1.80), "Winamax", BetStatus.WON),
            )
        paliers.forEachIndexed { index, (selection, bookmaker, result) ->
            playPalier(id, main, selection, bookmaker, at(MONTANTE_START_DAYS - index, "20:00"), result)
        }
        playPalier(
            id,
            main,
            tennis("Humbert – Moutet", "ATP Metz", "Humbert gagne", 2.10, "humbert-moutet"),
            "Betclic",
            at(0, "19:30"),
            null,
        )
    }

    private fun seedFinishedMontantes(main: Long) {
        val succeeded =
            montantes.create(
                MontanteConfig("Montante Tennis", main, Money.euros(50), 1.6, MontanteMode.STEPS, null, 3, false, 20, 0),
                at(60, "09:00"),
            )
        listOf("Rune gagne" to 1.55, "Zverev gagne" to 1.62, "Fritz gagne" to 1.70).forEachIndexed { index, (pick, odds) ->
            playPalier(
                succeeded,
                main,
                tennis("Match ATP ${index + 1}", "ATP", pick, odds),
                "Betclic",
                at(59L - index, "15:00"),
                BetStatus.WON,
            )
        }
        val broken =
            montantes.create(
                MontanteConfig("Montante Liga", main, Money.euros(40), 1.7, MontanteMode.OBJECTIVE, 5.0, null, true, 30, 0),
                at(40, "09:00"),
            )
        playPalier(
            broken,
            main,
            football("Valence – Osasuna", "Liga", "Résultat final", "Valence gagne", 1.72),
            "Unibet",
            at(39, "21:00"),
            BetStatus.WON,
        )
        playPalier(
            broken,
            main,
            football("Villarreal – Celta", "Liga", "Total de buts", "Plus de 2,5", 1.75),
            "Unibet",
            at(38, "21:00"),
            BetStatus.LOST,
        )
    }

    /** A montante bet stakes the whole current capital: read it back before placing. */
    private fun playPalier(
        montanteId: Long,
        bankrollId: Long,
        selection: Selection,
        bookmaker: String,
        startsAt: Instant,
        result: BetStatus?,
    ) {
        val montante = requireNotNull(montantes.find(montanteId))
        val state = MontanteEngine.replay(montante, bets.findAll().filter { it.montanteId == montanteId })
        val bet = NewBet(bankrollId, montanteId, BetType.SIMPLE, bookmaker, state.capital, selection.odds, startsAt, listOf(selection))
        record(bet, startsAt - Duration.ofHours(4), result)
    }

    private fun seedHistory(
        main: Long,
        funBankroll: Long,
    ) {
        for (daysAgo in HISTORY_DAYS downTo RECENT_DAYS step 2) {
            val template = HISTORY_TEMPLATES[random.nextInt(HISTORY_TEMPLATES.size)]
            val odds = template.minOdds + random.nextDouble() * (template.maxOdds - template.minOdds)
            val rounded = Math.round(odds * 100) / 100.0
            val won = random.nextDouble() < WIN_EDGE / rounded
            val hour = if (random.nextInt(LATE_ONE_IN) == 0) "22:30" else "20:00"
            val selection = Selection(null, template.event, template.sport, template.competition, template.market, template.pick, rounded)
            val stake = STAKES[random.nextInt(STAKES.size)]
            val placedBefore = Duration.ofHours(if (random.nextBoolean()) 2 else PLACED_DAY_BEFORE_HOURS)
            place(
                main,
                selection,
                stake,
                BOOKMAKERS[random.nextInt(BOOKMAKERS.size)],
                at(daysAgo, hour),
                if (won) BetStatus.WON else BetStatus.LOST,
                placedBefore = placedBefore,
            )
            if (daysAgo % LONG_SHOT_EVERY == 0L) {
                val longShot =
                    Selection(null, "Outsider du jour", "Football", "Ligue 2", "Résultat final", "Outsider gagne", LONG_SHOT_ODDS)
                val longShotWon = random.nextDouble() < LONG_SHOT_HIT_RATE
                place(funBankroll, longShot, FUN_STAKE, "Unibet", at(daysAgo, "18:00"), if (longShotWon) BetStatus.WON else BetStatus.LOST)
            }
        }
    }

    private fun football(
        event: String,
        competition: String,
        market: String,
        pick: String,
        odds: Double,
        eventId: String? = null,
    ) = Selection(eventId, event, "Football", competition, market, pick, odds)

    private fun tennis(
        event: String,
        competition: String,
        pick: String,
        odds: Double,
        eventId: String? = null,
    ) = Selection(eventId, event, "Tennis", competition, "Vainqueur", pick, odds)

    private data class Template(
        val sport: String,
        val competition: String,
        val event: String,
        val market: String,
        val pick: String,
        val minOdds: Double,
        val maxOdds: Double,
    )

    private companion object {
        const val SEED = 42
        const val HISTORY_DAYS = 100L
        const val RECENT_DAYS = 4L
        const val MONTANTE_START_DAYS = 5L
        const val MAX_STAKE_PCT = 3
        const val COMBO_ODDS = 4.35
        const val WIN_EDGE = 1.05
        const val LATE_ONE_IN = 5
        const val PLACED_DAY_BEFORE_HOURS = 26L
        const val LONG_SHOT_EVERY = 8L
        const val LONG_SHOT_ODDS = 4.5
        const val LONG_SHOT_HIT_RATE = 0.2
        const val FUN_STAKE = 5
        val STAKES = listOf(10, 15, 20, 25, 30)
        val BOOKMAKERS = listOf("Winamax", "Betclic", "Unibet")
        val HISTORY_TEMPLATES =
            listOf(
                Template("Football", "Ligue 1", "Nantes – Toulouse", "Total de buts", "Plus de 1,5", 1.4, 1.9),
                Template("Football", "Premier League", "Arsenal – Chelsea", "Résultat final", "Arsenal gagne", 1.6, 2.2),
                Template("Football", "Ligue 1", "Lille – Rennes", "Les deux équipes marquent", "Oui", 1.6, 1.9),
                Template("Tennis", "ATP", "Match ATP", "Vainqueur", "Favori gagne", 1.3, 2.4),
                Template("Basket", "NBA", "Celtics – Knicks", "Vainqueur", "Celtics", 1.5, 2.2),
                Template("Rugby", "Top 14", "Clermont – Castres", "Vainqueur", "Castres", 2.8, 3.6),
            )
    }
}
