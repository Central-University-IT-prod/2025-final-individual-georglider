package ru.georglider.prod.payload.request.time

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

data class TimeSetRequest(
    @field:JsonProperty("current_date") @field:NotNull val date: Int?
)
