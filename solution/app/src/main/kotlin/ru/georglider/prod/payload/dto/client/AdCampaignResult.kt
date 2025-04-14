package ru.georglider.prod.payload.dto.client

import org.springframework.data.relational.core.mapping.Column
import ru.georglider.prod.payload.response.client.AdResponse
import java.util.*

data class AdCampaignResult(
    @field:Column("campaign_id") var campaignId: UUID,
    @field:Column("advertiser_id") val advertiserId: UUID,
    @field:Column("impressions_limit") val impressionsLimit: Int,
    val hasImage: Boolean,
    val impressionsAmount: Int,
    @field:Column("cost_per_impression") var costPerImpression: Double,
    @field:Column("ad_title") val adTitle: String,
    @field:Column("ad_text") val adText: String,
) : CampaignResult {
    override fun toResponseAd(): AdResponse {
        return AdResponse(this.campaignId, this.adTitle, this.adText, this.advertiserId, this.hasImage)
    }
    override fun getIRevenue(): Double = costPerImpression
    override fun getIAdvertiserId(): UUID = advertiserId
    override fun getIAdId(): UUID = campaignId
}
