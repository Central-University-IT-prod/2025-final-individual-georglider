package ru.georglider.prod.repository.internal

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import ru.georglider.prod.model.advertiser.campaign.Campaign
import ru.georglider.prod.model.common.Gender
import ru.georglider.prod.model.internal.PaginationDetails
import ru.georglider.prod.model.moderator.CampaignUpdate
import ru.georglider.prod.model.moderator.ModerationRequest
import ru.georglider.prod.payload.dto.advertiser.campaign.GeneratedCampaignValuesHolder
import java.math.BigDecimal
import java.util.*
import java.util.function.BiFunction

@Repository
class ModerationRepository (
    private val databaseClient: DatabaseClient
) {

    companion object {
        val MODERATION_MAPPING_FUNCTION: BiFunction<Row, RowMetadata, ModerationRequest> =
            BiFunction<Row, RowMetadata, ModerationRequest> { row: Row, _: RowMetadata? ->
                ModerationRequest(
                    row.get("ad_title", String::class.java)!!,
                    row.get("ad_text", String::class.java)!!,
                    row.get("campaign_id", UUID::class.java)!!
                )
            }
        val NEW_CAMPAIGN_MAPPING_FUNCTION: BiFunction<Row, RowMetadata, Campaign> =
            BiFunction<Row, RowMetadata, Campaign> { row: Row, _: RowMetadata? ->
                Campaign(
                    row.get("advertiser_id", UUID::class.java)!!,
                    row.get("impressions_limit", Int::class.java)!!,
                    row.get("clicks_limit", Int::class.java)!!,
                    row.get("cost_per_impression", Double::class.java)!!,
                    row.get("cost_per_click", Double::class.java)!!,
                    row.get("ad_title", String::class.java)!!,
                    row.get("ad_text", String::class.java)!!,
                    row.get("start_date", Int::class.java)!!,
                    row.get("end_date", Int::class.java)!!,
                    row.get("gender", String::class.java)?.toShort()?.let { Gender.entries[it.toInt()] },
                    row.get("age_from", String::class.java)?.toInt(),
                    row.get("age_to", String::class.java)?.toInt(),
                    row.get("location", String::class.java),
                    row.get("campaign_id", UUID::class.java)!!,
                    false
                )
            }
        val UPDATE_MAPPING_FUNCTION: BiFunction<Row, RowMetadata, CampaignUpdate> =
            BiFunction<Row, RowMetadata, CampaignUpdate> { row: Row, t: RowMetadata ->
                CampaignUpdate(
                    row.get("ad_title", String::class.java)!!,
                    row.get("ad_text", String::class.java)!!,
                    row.get("campaign_id", UUID::class.java)!!,
                    if (row.get("is_new", Boolean::class.java)!!) NEW_CAMPAIGN_MAPPING_FUNCTION.apply(row, t) else null
                )
            }
    }

    fun insert(campaign: Campaign): Mono<GeneratedCampaignValuesHolder> = this.databaseClient
        .sql("INSERT INTO filtered_campaigns (advertiser_id, impressions_limit, clicks_limit, cost_per_impression, cost_per_click, ad_title, ad_text, start_date, end_date, gender, age_from, age_to, location, is_new)" +
                " VALUES (:advertiser_id, :impressions_limit, :clicks_limit, :cost_per_impression, :cost_per_click, :ad_title, :ad_text, :start_date, :end_date, :gender, :age_from, :age_to, :location, true)")
        .bindValues(campaign.values())
        .filter { statement, next ->
            next.execute(statement.returnGeneratedValues("campaign_id", "cost_per_impression", "cost_per_click"))
        }
        .map { row, _ ->
            GeneratedCampaignValuesHolder(
                row.get("campaign_id") as UUID,
                row.get("cost_per_impression") as BigDecimal,
                row.get("cost_per_click") as BigDecimal,
                false
            )
        }
        .first()

    fun upsert(campaign: Campaign) = this.databaseClient
        .sql("""
            INSERT INTO filtered_campaigns (campaign_id, advertiser_id, impressions_limit, clicks_limit, cost_per_impression, cost_per_click, ad_title, ad_text, start_date, end_date, gender, age_from, age_to, location, is_new)
                VALUES (:campaign_id, :advertiser_id, :impressions_limit, :clicks_limit, :cost_per_impression, :cost_per_click, :ad_title, :ad_text, :start_date, :end_date, :gender, :age_from, :age_to, :location, false)
                ON CONFLICT (campaign_id) DO UPDATE SET advertiser_id = :advertiser_id, impressions_limit = :impressions_limit, clicks_limit = :clicks_limit,
                cost_per_impression = :cost_per_impression, cost_per_click = :cost_per_click, ad_title = :ad_title,
                ad_text = :ad_text, start_date = :start_date, end_date = :end_date, gender = :gender, 
                age_from = :age_from, age_to = :age_to, location = :location
        """.trimIndent())
        .bindValues(campaign.values())
        .bind("campaign_id", campaign.campaignId!!)
        .fetch()
        .rowsUpdated()
    
    fun findWithPagination(pagination: PaginationDetails) = this.databaseClient
        .sql("SELECT * FROM filtered_campaigns offset :offset limit :limit")
        .bind("offset", pagination.offset())
        .bind("limit", pagination.size)
        .map(MODERATION_MAPPING_FUNCTION)
        .all()

    fun findOne() = this.databaseClient
        .sql("SELECT * FROM filtered_campaigns limit 1")
        .map(MODERATION_MAPPING_FUNCTION)
        .one()

    fun findById(id: UUID) = this.databaseClient
        .sql("SELECT * from filtered_campaigns WHERE campaign_id = :id")
        .bind("id", id)
        .map(UPDATE_MAPPING_FUNCTION)
        .one()

    fun deleteByCampaignId(campaignId: UUID) = this.databaseClient
        .sql("DELETE FROM filtered_campaigns WHERE campaign_id = :campaign_id")
        .bind("campaign_id", campaignId)
        .fetch()
        .rowsUpdated()

    fun deleteByAdvertiserIdAndCampaignId(advertiserId: UUID, campaignId: UUID) = this.databaseClient
        .sql("DELETE FROM filtered_campaigns WHERE advertiser_id = :advertiser_id AND campaign_id = :campaign_id")
        .bind("advertiser_id", advertiserId)
        .bind("campaign_id", campaignId)
        .fetch()
        .rowsUpdated()


}