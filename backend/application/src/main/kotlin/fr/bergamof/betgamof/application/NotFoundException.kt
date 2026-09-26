package fr.bergamof.betgamof.application

/** The requested bankroll, bet, montante or rule does not exist. Business rule violations are domain exceptions. */
class NotFoundException(
    message: String,
) : RuntimeException(message)
