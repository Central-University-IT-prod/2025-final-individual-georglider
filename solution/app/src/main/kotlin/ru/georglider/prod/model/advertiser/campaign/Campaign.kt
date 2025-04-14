package ru.georglider.prod.model.advertiser.campaign

import io.r2dbc.spi.Parameters
import io.r2dbc.spi.R2dbcType
import org.springframework.data.relational.core.mapping.Column
import ru.georglider.prod.model.common.Gender
import ru.georglider.prod.payload.request.advertiser.campaign.CampaignRequest
import java.util.*

data class Campaign(
    @field:Column("advertiser_id") val advertiserId: UUID,
    @field:Column("impressions_limit") val impressionsLimit: Int,
    @field:Column("clicks_limit") val clicksLimit: Int,
    @field:Column("cost_per_impression") var costPerImpression: Double,
    @field:Column("cost_per_click") var costPerClick: Double,
    @field:Column("ad_title") var adTitle: String,
    @field:Column("ad_text") var adText: String,
    @field:Column("start_date") val startDate: Int,
    @field:Column("end_date") val endDate: Int,
    @field:Column("gender") val gender: Gender?,
    @field:Column("age_from") val ageFrom: Int?,
    @field:Column("age_to") val ageTo: Int?,
    @field:Column("location") val location: String?,
    @field:Column("campaign_id") var campaignId: UUID? = null,
    @field:Column("has_image") var hasImage: Boolean = false
) {

    constructor(advertiserId: UUID, campaignId: UUID?, campaign: CampaignRequest) : this(advertiserId,
        campaign.getIImpressionsLimit()!!, campaign.getIClicksLimit()!!, campaign.getICostPerImpression()!!, campaign.getICostPerClick()!!,
        campaign.getIAdTitle()!!, campaign.getIAdText()!!, campaign.getIStartDate()!!, campaign.getIEndDate()!!,
        campaign.getITargeting()?.gender.let { if (it != null) Gender.valueOf(it) else null },
        campaign.getITargeting()?.ageFrom, campaign.getITargeting()?.ageTo, campaign.getITargeting()?.location, campaignId
    )

    constructor(advertiserId: UUID, campaign: CampaignRequest) : this(advertiserId, null, campaign)

    fun values(): Map<String, *> {
        return mapOf(
            "advertiser_id" to advertiserId,
            "impressions_limit" to impressionsLimit,
            "clicks_limit" to clicksLimit,
            "cost_per_impression" to costPerImpression,
            "cost_per_click" to costPerClick,
            "ad_title" to adTitle,
            "ad_text" to adText,
            "start_date" to startDate,
            "end_date" to endDate,
            "gender" to Parameters.`in`(R2dbcType.SMALLINT, gender?.ordinal?.toShort()),
            "age_from" to Parameters.`in`(R2dbcType.INTEGER, ageFrom),
            "age_to" to Parameters.`in`(R2dbcType.INTEGER, ageTo),
            "location" to Parameters.`in`(R2dbcType.VARCHAR, location)
        )
    }

}
