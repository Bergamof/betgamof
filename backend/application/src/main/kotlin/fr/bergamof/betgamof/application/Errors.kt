package fr.bergamof.betgamof.application

class NotFoundException(
    message: String,
) : RuntimeException(message)

/** The request is valid but conflicts with the current state (e.g. settling a settled bet). */
class ConflictException(
    message: String,
) : RuntimeException(message)
