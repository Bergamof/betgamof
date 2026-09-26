package fr.bergamof.betgamof.domain

/** A business rule was broken. Anything else thrown by the domain is a programming error. */
sealed class DomainException(
    message: String,
) : RuntimeException(message)

/** A value does not satisfy the domain's constraints (negative stake, odds below 1, blank name...). */
class InvalidValueException(
    message: String,
) : DomainException(message)

/** The requested change is not allowed in the aggregate's current state (settling a settled bet...). */
class InvalidTransitionException(
    message: String,
) : DomainException(message)

internal inline fun ensureValid(
    condition: Boolean,
    message: () -> String,
) {
    if (!condition) throw InvalidValueException(message())
}

internal inline fun ensureTransition(
    condition: Boolean,
    message: () -> String,
) {
    if (!condition) throw InvalidTransitionException(message())
}
