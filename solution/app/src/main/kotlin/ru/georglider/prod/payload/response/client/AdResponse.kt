package ru.georglider.prod.payload.response.client

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class AdResponse(
    @field:JsonProperty("ad_id") val adId: UUID,
    @field:JsonProperty("ad_title") val adTitle: String,
    @field:JsonProperty("ad_text") val adText: String,
    @field:JsonProperty("advertiser_id") val advertiserId: UUID,
    @field:JsonProperty("image_url") val imageUrl: String?
) {
    constructor(adId: UUID, adTitle: String, adText: String, advertiserId: UUID, imageStatus: Boolean) :
            this(adId, adTitle, adText, advertiserId, if (imageStatus) "/ads/$adId/image" else null)
}
