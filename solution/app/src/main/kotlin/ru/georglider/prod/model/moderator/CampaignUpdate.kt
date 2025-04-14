package ru.georglider.prod.model.moderator

import ru.georglider.prod.model.advertiser.campaign.Campaign
import java.util.*

data class CampaignUpdate(
    val adTitle: String,
    val adText: String,
    val id: UUID,
    val campaign: Campaign?
)
