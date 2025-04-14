package ru.georglider.prod.payload.dto.client

import ru.georglider.prod.payload.response.client.AdResponse
import java.util.*

data class RankedAd(
    val adId: UUID,
    val adTitle: String,
    val adText: String,
    val advertiserId: UUID,
    val hasImage: Boolean,
    val rank: Int,
    val impressionsAmount: Int,
    val maxImpressions: Int,
    val revenue: Double
) : CampaignResult {
    override fun toResponseAd(): AdResponse {
        return AdResponse(this.adId, this.adTitle, this.adText, this.advertiserId, this.hasImage)
    }
    override fun getIRevenue(): Double = revenue
    override fun getIAdvertiserId(): UUID = advertiserId
    override fun getIAdId(): UUID = adId
}
