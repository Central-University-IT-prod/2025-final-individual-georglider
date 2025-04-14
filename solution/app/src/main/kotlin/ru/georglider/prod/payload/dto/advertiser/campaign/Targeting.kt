package ru.georglider.prod.payload.dto.advertiser.campaign

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Targeting (
    @field:JsonProperty("gender") val gender: String? = null,
    @field:JsonProperty("age_from") val ageFrom: Int? = null,
    @field:JsonProperty("age_to") val ageTo: Int? = null,
    @field:JsonProperty("location") val location: String? = null
) {
    @JsonIgnore
    fun isValid(): Boolean {
        val vGender = gender == null || arrayOf("MALE", "FEMALE", "ALL").contains(gender)
        val vAge = (ageFrom ?: Int.MIN_VALUE) <= (ageTo ?: Int.MAX_VALUE)
        val vAgeTo = (ageTo ?: Int.MAX_VALUE) >= 0
        val validLocation = location == null || location.length < 256
        return vGender && vAge && vAgeTo && validLocation
    }
}
