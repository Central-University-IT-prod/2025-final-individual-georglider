package ru.georglider.prod.model.advertiser.campaign.stats

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("stat_records")
data class StatRecord (
    @Column("advertiser_id") val advertiserId: UUID,
    @Column("campaign_id") val campaignId: UUID,
    @Column("spent") val spent: Double,
    @Column("type") val type: RecordType,
    @Column("date") var date: Int? = null,
    @Column("id") val id: Int? = null
)