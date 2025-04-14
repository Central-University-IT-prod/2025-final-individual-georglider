package ru.georglider.prod.payload.request.moderator

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class DecisionRequest(
    @field:NotNull @field:JsonProperty("campaign_id") val campaignId: UUID
)
