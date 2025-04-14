package ru.georglider.prod.payload.response.client

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class AdClickResponse(
    @field:JsonProperty("client_id") val clientId: UUID
)
