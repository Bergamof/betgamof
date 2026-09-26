package fr.bergamof.betgamof.domain

/**
 * Rebuilds a montante's state by replaying its bets in order.
 * Nothing but the configuration, the bets and the manual closing date is stored.
 */
object MontanteEngine {
    fun replay(
        montante: Montante,
        bets: List<Bet>,
    ): MontanteState {
        val config = montante.config
        val plan = MontantePlanner.plan(config)
        val replay = Replay(config, plan)
        bets.sortedBy { it.placedAt }.forEach(replay::apply)
        val status =
            when {
                replay.outcome != MontanteStatus.ACTIVE -> replay.outcome
                montante.closedAt != null -> MontanteStatus.CLOSED
                else -> MontanteStatus.ACTIVE
            }
        return MontanteState(
            status = status,
            capital = replay.capital,
            engaged = replay.engaged,
            secured = replay.secured,
            relancesUsed = replay.relancesUsed,
            winsInRun = replay.winsInRun,
            paliers = replay.paliers,
            openBet = replay.openBet,
            nextIsRelance = replay.nextIsRelance,
            target = plan.target,
            plannedSteps = plan.plannedSteps,
        )
    }

    private class Replay(
        private val config: MontanteConfig,
        private val plan: MontantePlan,
    ) {
        var capital = config.startCapital
        var engaged = config.startCapital
        var secured = Money.ZERO
        var relancesUsed = 0
        var outcome = MontanteStatus.ACTIVE
        var openBet: Bet? = null
        var nextIsRelance = false
        val paliers = mutableListOf<Palier>()
        var winsInRun = 0
            private set

        fun apply(bet: Bet) {
            if (bet.isOpen) {
                openBet = bet
                return
            }
            val before = capital
            val isRelance = nextIsRelance
            nextIsRelance = false
            var palierSecured = Money.ZERO
            when (bet.status) {
                BetStatus.WON -> palierSecured = win(bet.odds)
                BetStatus.CASHOUT -> palierSecured = cashout(bet)
                BetStatus.LOST -> lose()
                BetStatus.VOID, BetStatus.OPEN -> Unit
            }
            paliers += Palier(paliers.size + 1, bet, before, palierSecured, capital, isRelance)
            checkObjective()
        }

        private fun win(odds: Double): Money {
            val gain = MontantePlanner.winPalier(capital, odds, config.secureRatio)
            capital = gain.capitalAfter
            secured += gain.secured
            winsInRun++
            return gain.secured
        }

        private fun cashout(bet: Bet): Money {
            val returned = bet.cashout ?: Money.ZERO
            return if (returned > capital) {
                win(returned / capital)
            } else {
                capital = returned
                Money.ZERO
            }
        }

        private fun lose() {
            if (config.allowsRelance(relancesUsed)) {
                relancesUsed++
                engaged += config.startCapital
                capital = config.startCapital
                winsInRun = 0
                nextIsRelance = true
            } else {
                capital = Money.ZERO
                outcome = MontanteStatus.BROKEN
            }
        }

        private fun checkObjective() {
            if (outcome != MontanteStatus.ACTIVE) return
            val reached =
                when (config.mode) {
                    MontanteMode.OBJECTIVE -> capital >= requireNotNull(plan.target)
                    MontanteMode.STEPS -> winsInRun >= requireNotNull(config.stepCount)
                    MontanteMode.FREE -> false
                }
            if (reached) outcome = MontanteStatus.SUCCEEDED
        }
    }
}
