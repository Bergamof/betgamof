package fr.bergamof.betgamof.business.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DefaultStakeTest {
    @Test
    fun `a percentage may go up to the whole balance`() {
        assertEquals(100.0, DefaultStake.Percent(100.0).percent)
    }

    @Test
    fun `a percentage cannot exceed the whole balance`() {
        assertFailsWith<IllegalArgumentException> { DefaultStake.Percent(100.5) }
    }
}
