package ru.georglider.prod.payload.dto.internal

import ru.georglider.prod.model.advertiser.campaign.stats.RecordType

data class DbDateStats(
    val sum: Double,
    val count: Int,
    val date: Int,
    val type: RecordType
)
