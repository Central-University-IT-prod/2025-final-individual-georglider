package ru.georglider.prod.payload.dto.advertiser

import com.fasterxml.jackson.annotation.JsonProperty
import ru.georglider.prod.model.advertiser.Advertiser
import java.util.*

data class AdvertiserDTO (
    @field:JsonProperty("advertiser_id") val advertiserId: UUID,
    @field:JsonProperty("name") val name: String,
) {
    constructor (advertiser: Advertiser) : this(advertiser.advertiserId, advertiser.name)
}