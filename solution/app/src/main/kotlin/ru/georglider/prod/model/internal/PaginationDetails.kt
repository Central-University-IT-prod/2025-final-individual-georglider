package ru.georglider.prod.model.internal

data class PaginationDetails(
    val page: Int,
    val size: Int
) {
    fun offset() = (page - 1) * size
}
