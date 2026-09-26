package fr.bergamof.betgamof.domain

import kotlin.math.roundToLong

private const val CENTS_PER_EURO = 100.0

/** An amount in euros, stored as cents to avoid floating-point drift on balances. */
@JvmInline
value class Money(
    val cents: Long,
) : Comparable<Money> {
    operator fun plus(other: Money) = Money(cents + other.cents)

    operator fun minus(other: Money) = Money(cents - other.cents)

    operator fun unaryMinus() = Money(-cents)

    operator fun times(factor: Double) = Money((cents * factor).roundToLong())

    operator fun div(other: Money): Double = cents.toDouble() / other.cents

    override fun compareTo(other: Money) = cents.compareTo(other.cents)

    val isPositive get() = cents > 0

    fun toEuros(): Double = cents / CENTS_PER_EURO

    companion object {
        val ZERO = Money(0)

        fun euros(amount: Double) = Money((amount * CENTS_PER_EURO).roundToLong())

        fun euros(amount: Int) = Money(amount * CENTS_PER_EURO.toLong())
    }
}

fun Iterable<Money>.sum(): Money = fold(Money.ZERO) { total, amount -> total + amount }
