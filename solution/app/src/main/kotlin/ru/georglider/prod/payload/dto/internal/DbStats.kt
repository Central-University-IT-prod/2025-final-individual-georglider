package ru.georglider.prod.payload.dto.internal

import ru.georglider.prod.model.advertiser.campaign.stats.RecordType

data class DbStats(
    val sum: Double,
    val count: Int,
    val type: RecordType
)
