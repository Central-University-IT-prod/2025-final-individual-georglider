package ru.georglider.prod.repository.advertiser.stats

import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import ru.georglider.prod.model.advertiser.campaign.stats.StatRecord

@Repository
interface StatRecordRepository : R2dbcRepository<StatRecord, Int>