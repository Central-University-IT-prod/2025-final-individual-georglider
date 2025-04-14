package ru.georglider.prod.repository.advertiser.stats

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import ru.georglider.prod.model.advertiser.campaign.stats.RecordType
import ru.georglider.prod.payload.dto.internal.DbDateStats
import ru.georglider.prod.payload.dto.internal.DbStats
import java.util.UUID
import java.util.function.BiFunction

@Repository
class AdvancedStatRecordRepository (
    private val databaseClient: DatabaseClient
) {

    companion object {
        val MAPPING_FUNCTION: BiFunction<Row, RowMetadata, DbStats> =
            BiFunction<Row, RowMetadata, DbStats> { row: Row, _: RowMetadata? ->
                DbStats(
                    row.get("sum", Double::class.java)!!,
                    row.get("count", Int::class.java)!!,
                    row.get("type", Short::class.java)!!.let { RecordType.entries[it.toInt()] },
                )
            }

        val MAPPING_FUNCTION_DATE: BiFunction<Row, RowMetadata, DbDateStats> =
            BiFunction<Row, RowMetadata, DbDateStats> { row: Row, _: RowMetadata? ->
                DbDateStats(
                    row.get("sum", Double::class.java)!!,
                    row.get("count", Int::class.java)!!,
                    row.get("date", Int::class.java)!!,
                    row.get("type", Short::class.java)!!.let { RecordType.entries[it.toInt()] },
                )
            }
    }

    fun retrieveStatsByCampaignId(campaignId: UUID) = this.databaseClient
        .sql("SELECT SUM (spent), count(type), type FROM stat_records " +
                "WHERE campaign_id = :campaign_id GROUP BY type")
        .bind(0, campaignId)
        .map(MAPPING_FUNCTION)
        .all()

    fun retrieveStatsByAdvertiserId(advertiserId: UUID) = this.databaseClient
        .sql("SELECT SUM (spent), count(type), type FROM stat_records " +
                "WHERE advertiser_id = :advertiser_id GROUP BY type")
        .bind(0, advertiserId)
        .map(MAPPING_FUNCTION)
        .all()

    fun retrieveDailyStatsByCampaignId(campaignId: UUID) = this.databaseClient
        .sql("SELECT SUM (spent), count(type), date, type FROM stat_records " +
                "WHERE campaign_id = :campaign_id GROUP BY date, type")
        .bind(0, campaignId)
        .map(MAPPING_FUNCTION_DATE)
        .all()

    fun retrieveDailyStatsByAdvertiserId(advertiserId: UUID) = this.databaseClient
        .sql("SELECT SUM (spent), count(type), date, type FROM stat_records " +
                "WHERE advertiser_id = :advertiser_id GROUP BY date, type")
        .bind(0, advertiserId)
        .map(MAPPING_FUNCTION_DATE)
        .all()

}