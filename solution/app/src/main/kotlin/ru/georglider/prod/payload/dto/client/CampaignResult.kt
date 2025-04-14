package ru.georglider.prod.payload.dto.client

import ru.georglider.prod.payload.response.client.AdResponse
import java.util.*

interface CampaignResult {

    fun toResponseAd(): AdResponse
    fun getIRevenue(): Double
    fun getIAdvertiserId(): UUID
    fun getIAdId(): UUID

}