package ru.georglider.prod.payload.request.client

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class AdRequest(
    @field:NotNull @field:JsonProperty("client_id") val clientId: UUID? = null
)
