package ru.georglider.prod.payload.request.client

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.*

data class BulkEntity(
    @field:JsonProperty("client_id") @field:NotNull val clientId: UUID? = null,
    @field:JsonProperty("login") @field:NotNull @field:NotBlank(message = "Поле логин не может быть пустым")
    @field:Size(max = 255, message = "Поле логин не может превышать 255 символов") val login: String? = null,
    @field:JsonProperty("age") @field:NotNull  @field:Min(0)
    @field:Max(122, message = "Возраст не может превышать 122 года") val age: Int? = null,
    @field:JsonProperty("location") @field:NotNull @field:NotBlank(message = "Поле локация не может быть пустым")
    @field:Size(max = 255, message = "Поле локация не может превышать 255 символов") val location: String? = null,
    @field:JsonProperty("gender") @field:NotNull val gender: String? = null
) {

    @JsonIgnore
    fun isValid() = arrayOf("MALE", "FEMALE").contains(gender)

}