package ru.georglider.prod.payload.dto.advertiser.campaign

import java.math.BigDecimal
import java.util.*

data class GeneratedCampaignValuesHolder(
    val campaignId: UUID,
    val costPerImpression: Double,
    val costPerClick: Double,
    val hasImage: Boolean
) {
    constructor(campaignId: UUID, costPerImpression: BigDecimal, costPerClick: BigDecimal, hasImage: Boolean) :
            this(campaignId, costPerImpression.toDouble(), costPerClick.toDouble(), hasImage)
}
