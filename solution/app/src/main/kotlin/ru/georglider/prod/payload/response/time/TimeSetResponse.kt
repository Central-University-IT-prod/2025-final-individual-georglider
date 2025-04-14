package ru.georglider.prod.payload.response.time

import com.fasterxml.jackson.annotation.JsonProperty

data class TimeSetResponse(
    @field:JsonProperty("current_date") val date: Int
)
