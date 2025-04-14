package ru.georglider.prod.payload.dto.client

import com.fasterxml.jackson.annotation.JsonProperty
import ru.georglider.prod.model.client.Client
import ru.georglider.prod.model.common.Gender
import java.util.*

data class ClientDTO (
    @field:JsonProperty("client_id") val clientId: UUID,
    @field:JsonProperty("login") val login: String,
    @field:JsonProperty("age") val age: Int,
    @field:JsonProperty("location") val location: String,
    @field:JsonProperty("gender") val gender: Gender
) {
    constructor (client: Client) : this(client.clientId, client.login, client.age, client.location, client.gender)
}