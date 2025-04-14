package ru.georglider.prod.model.moderator

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class ModerationRequest(
    @field:JsonProperty("ad_title") val adTitle: String,
    @field:JsonProperty("ad_text") val adText: String,
    @field:JsonProperty("campaign_id") val id: UUID
)
