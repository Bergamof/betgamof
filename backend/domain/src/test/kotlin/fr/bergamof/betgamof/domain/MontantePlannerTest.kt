package fr.bergamof.betgamof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MontantePlannerTest {
    @Test
    fun `objective mode plans enough paliers to reach the target`() {
        val plan = MontantePlanner.plan(montanteConfig(targetMultiplier = 3.0))

        assertEquals(Money.euros(480), plan.target)
        assertEquals(3, plan.plannedSteps)
        assertTrue(plan.steps.last().capitalAfter >= Money.euros(480))
        assertEquals(Money.euros(244), plan.steps.first().capitalAfter)
    }

    @Test
    fun `success probability multiplies implied probabilities`() {
        assertEquals(1 / (2.0 * 2.0), MontantePlanner.successProbability(2.0, 2), 1e-9)
    }

    @Test
    fun `required odds reach the target in the remaining paliers`() {
        val odds = assertNotNull(MontantePlanner.requiredOdds(Money.euros(100), Money.euros(400), 2, 0.0))

        assertEquals(2.0, odds, 1e-9)
        assertNull(MontantePlanner.requiredOdds(Money.euros(500), Money.euros(400), 2, 0.0))
    }

    @Test
    fun `invalid configurations are rejected`() {
        assertFailsWith<IllegalArgumentException> { montanteConfig(targetMultiplier = 1.0) }
        assertFailsWith<IllegalArgumentException> { montanteConfig(mode = MontanteMode.STEPS, stepCount = 0) }
        assertFailsWith<IllegalArgumentException> { montanteConfig(securePct = 95) }
    }
}
