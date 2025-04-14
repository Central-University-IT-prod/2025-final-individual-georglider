package ru.georglider.prod.payload.request.advertiser

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.*

data class BulkEntity(
    @field:JsonProperty("advertiser_id") @field:NotNull val advertiserId: UUID? = null,
    @field:JsonProperty("name") @field:NotBlank(message = "Поле имя не может быть пустым")
    @field:NotNull @field:Size(max = 255, message = "Поле имя не может превышать 255 символов") val name: String? = null,
)
