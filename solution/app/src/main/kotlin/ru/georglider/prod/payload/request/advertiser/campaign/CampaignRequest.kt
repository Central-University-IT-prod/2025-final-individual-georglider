package ru.georglider.prod.payload.request.advertiser.campaign

import ru.georglider.prod.payload.dto.advertiser.campaign.Targeting

interface CampaignRequest {

    fun getIImpressionsLimit(): Int?
    fun getIClicksLimit(): Int?
    fun getICostPerImpression(): Double?
    fun getICostPerClick(): Double?
    fun getIAdTitle(): String?
    fun getIAdText(): String?
    fun getIStartDate(): Int?
    fun getIEndDate(): Int?
    fun getITargeting(): Targeting?

}