package ru.georglider.prod.payload.dto.advertiser.stats

import com.fasterxml.jackson.annotation.JsonProperty
import ru.georglider.prod.payload.dto.internal.DbDateStats

data class DateStats(
    @JsonProperty("date") val date: Int,
    @JsonProperty("impressions_count") val impressionsCount: Int,
    @JsonProperty("clicks_count") val clicksCount: Int,
    @JsonProperty("spent_impressions") val spentImpressions: Double,
    @JsonProperty("spent_clicks") val spentClicks: Double,
    @JsonProperty("conversion") val conversion: Double = if (impressionsCount == 0) 0.00 else (clicksCount.toDouble() / impressionsCount.toDouble() * 100.0),
    @JsonProperty("spent_total") val spentTotal: Double = spentImpressions + spentClicks
) {
    constructor(date: Int, impressions: List<DbDateStats>, clicks: List<DbDateStats>) : this(
        date,
        impressions.fold(0) { acc, n -> acc + n.count },
        clicks.fold(0) { acc, n -> acc + n.count },
        impressions.fold(0.0) { acc, n -> acc + n.sum },
        clicks.fold(0.0) { acc, n -> acc + n.sum }
    )
}