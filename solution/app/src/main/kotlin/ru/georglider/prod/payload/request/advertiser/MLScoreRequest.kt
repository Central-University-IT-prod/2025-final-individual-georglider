package ru.georglider.prod.payload.request.advertiser

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull
import java.util.*

data class MLScoreRequest(
    @field:NotNull @field:JsonProperty("client_id") val clientId: UUID?,
    @field:NotNull @field:JsonProperty("advertiser_id") val advertiserId: UUID?,
    @field:NotNull @field:JsonProperty("score") val score: Int?
)
