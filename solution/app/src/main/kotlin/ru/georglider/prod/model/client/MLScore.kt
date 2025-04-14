package ru.georglider.prod.model.client

import org.springframework.data.relational.core.mapping.Table
import ru.georglider.prod.payload.request.advertiser.MLScoreRequest
import java.util.*

@Table("mlscore")
data class MLScore(
    val clientId: UUID,
    val advertiserId: UUID,
    val mlScore: Int
) {
    constructor(request: MLScoreRequest) : this(request.clientId!!, request.advertiserId!!, request.score!!)
}
