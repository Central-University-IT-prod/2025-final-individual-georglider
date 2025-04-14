package ru.georglider.prod.payload.dto.advertiser.campaign

import com.fasterxml.jackson.annotation.JsonProperty
import ru.georglider.prod.model.advertiser.campaign.Campaign
import java.util.*

data class CampaignDTO(
    @field:JsonProperty("campaign_id") val campaignId: UUID,
    @field:JsonProperty("advertiser_id") val advertiserId: UUID,
    @field:JsonProperty("impressions_limit") val impressionsLimit: Int,
    @field:JsonProperty("clicks_limit") val clicksLimit: Int,
    @field:JsonProperty("cost_per_impression") val costPerImpression: Double,
    @field:JsonProperty("cost_per_click") val costPerClick: Double,
    @field:JsonProperty("ad_title") val adTitle: String,
    @field:JsonProperty("ad_text") val adText: String,
    @field:JsonProperty("start_date") val startDate: Int,
    @field:JsonProperty("end_date") val endDate: Int,
    @field:JsonProperty("targeting") val targeting: Targeting,
    @field:JsonProperty("image_url") val imageUrl: String?
) {

    constructor(campaign: Campaign) : this(
        campaign.campaignId!!, campaign.advertiserId, campaign.impressionsLimit, campaign.clicksLimit,
        campaign.costPerImpression, campaign.costPerClick, campaign.adTitle, campaign.adText, campaign.startDate,
        campaign.endDate, Targeting(campaign.gender?.name, campaign.ageFrom, campaign.ageTo, campaign.location),
        if (campaign.hasImage) "/ads/${campaign.campaignId}/image" else null
    )

}