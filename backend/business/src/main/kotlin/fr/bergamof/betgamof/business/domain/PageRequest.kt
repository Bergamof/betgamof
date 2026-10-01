package fr.bergamof.betgamof.business.domain

/** A slice of a long list: at most [limit] items, after skipping [offset] of them. */
data class PageRequest(
    val limit: Int,
    val offset: Int = 0,
) {
    init {
        require(limit > 0) { "La taille de page doit être positive" }
        require(offset >= 0) { "Le décalage de page doit être positif ou nul" }
    }
}
