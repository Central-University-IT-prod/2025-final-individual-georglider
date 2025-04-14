package ru.georglider.prod.repository

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.bind
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import ru.georglider.prod.model.advertiser.campaign.Campaign
import ru.georglider.prod.model.common.Gender
import ru.georglider.prod.model.internal.PaginationDetails
import ru.georglider.prod.model.moderator.CampaignUpdate
import ru.georglider.prod.payload.dto.advertiser.campaign.GeneratedCampaignValuesHolder
import java.math.BigDecimal
import java.util.UUID
import java.util.function.BiFunction

@Repository
class CampaignRepository (
    private val databaseClient: DatabaseClient
) {

    companion object {
        val MAPPING_FUNCTION: BiFunction<Row, RowMetadata, Campaign> =
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
                    row.get("has_image", Boolean::class.java)!!,
                )
            }
    }

    fun insert(campaign: Campaign): Mono<GeneratedCampaignValuesHolder> = this.databaseClient
        .sql("INSERT INTO campaigns (advertiser_id, impressions_limit, clicks_limit, cost_per_impression, cost_per_click, ad_title, ad_text, start_date, end_date, gender, age_from, age_to, location)" +
                " VALUES (:advertiser_id, :impressions_limit, :clicks_limit, :cost_per_impression, :cost_per_click, :ad_title, :ad_text, :start_date, :end_date, :gender, :age_from, :age_to, :location)")
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

    fun insertWithId(campaign: Campaign): Mono<GeneratedCampaignValuesHolder> = this.databaseClient
        .sql("INSERT INTO campaigns (campaign_id, advertiser_id, impressions_limit, clicks_limit, cost_per_impression, cost_per_click, ad_title, ad_text, start_date, end_date, gender, age_from, age_to, location)" +
                " VALUES (:campaign_id, :advertiser_id, :impressions_limit, :clicks_limit, :cost_per_impression, :cost_per_click, :ad_title, :ad_text, :start_date, :end_date, :gender, :age_from, :age_to, :location)")
        .bindValues(campaign.values())
        .bind("campaign_id", campaign.campaignId)
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

    fun mergeUpdate(moderation: CampaignUpdate) = this.databaseClient
        .sql("UPDATE campaigns SET ad_text = :ad_text, ad_title = :ad_title WHERE campaign_id = :campaign_id")
        .bind("ad_text", moderation.adText)
        .bind("ad_title", moderation.adTitle)
        .bind("campaign_id", moderation.id)
        .fetch()
        .rowsUpdated()

    fun updateImage(campaignId: UUID, status: Boolean) = this.databaseClient
        .sql("UPDATE campaigns SET has_image = :status WHERE campaign_id = :campaign_id")
        .bind("status", status)
        .bind("campaign_id", campaignId)
        .fetch()
        .rowsUpdated()

    fun update(campaign: Campaign) = this.databaseClient
        .sql("UPDATE campaigns SET advertiser_id = :advertiser_id, impressions_limit = :impressions_limit, clicks_limit = :clicks_limit, " +
                "cost_per_impression = :cost_per_impression, cost_per_click = :cost_per_click, ad_title = :ad_title, " +
                "ad_text = :ad_text, start_date = :start_date, end_date = :end_date, gender = :gender, " +
                "age_from = :age_from, age_to = :age_to, location = :location WHERE campaign_id = :campaign_id")
        .bindValues(campaign.values())
        .bind("campaign_id", campaign.campaignId)
        .filter { statement, next ->
            next.execute(statement.returnGeneratedValues("campaign_id", "cost_per_impression", "cost_per_click", "has_image"))
        }
        .map { row, _ ->
            GeneratedCampaignValuesHolder(
                row.get("campaign_id") as UUID,
                row.get("cost_per_impression") as BigDecimal,
                row.get("cost_per_click") as BigDecimal,
                row.get("has_image") as Boolean
            )
        }
        .first()

    fun findByAdvertiserIdWithPagination(advertiserId: UUID, pagination: PaginationDetails) = this.databaseClient
        .sql("SELECT * FROM campaigns WHERE advertiser_id = :advertiser_id offset :offset limit :limit")
        .bind("advertiser_id", advertiserId)
        .bind("offset", pagination.offset())
        .bind("limit", pagination.size)
        .map(MAPPING_FUNCTION)
        .all()

    fun findByAdvertiserIdAndCampaignId(advertiserId: UUID, campaignId: UUID) = this.databaseClient
        .sql("SELECT * FROM campaigns WHERE advertiser_id = :advertiser_id AND campaign_id = :campaign_id")
        .bind("advertiser_id", advertiserId)
        .bind("campaign_id", campaignId)
        .map(MAPPING_FUNCTION)
        .one()

    fun existsByAdvertiserIdAndCampaignId(advertiserId: UUID, campaignId: UUID) = this.databaseClient
        .sql("SELECT 1 FROM campaigns WHERE advertiser_id = :advertiser_id AND campaign_id = :campaign_id LIMIT 1")
        .bind("advertiser_id", advertiserId)
        .bind("campaign_id", campaignId)
        .map { _, _ -> true }
        .one()

    fun existsByCampaignId(campaignId: UUID) = this.databaseClient
        .sql("SELECT 1 FROM campaigns WHERE campaign_id = :campaign_id LIMIT 1")
        .bind("campaign_id", campaignId)
        .map { _, _ -> true }
        .one()

    fun findByCampaignId(campaignId: UUID) = this.databaseClient
        .sql("SELECT * FROM campaigns WHERE campaign_id = :campaign_id")
        .bind("campaign_id", campaignId)
        .map(MAPPING_FUNCTION)
        .one()

    fun findAllByAdvertiserId(advertiserId: UUID) = this.databaseClient
        .sql("SELECT * FROM campaigns WHERE advertiser_id = :advertiser_id")
        .bind("advertiser_id", advertiserId)
        .map(MAPPING_FUNCTION)
        .all()

    fun deleteByAdvertiserIdAndCampaignId(advertiserId: UUID, campaignId: UUID) = this.databaseClient
        .sql("DELETE FROM campaigns WHERE advertiser_id = :advertiser_id AND campaign_id = :campaign_id")
        .bind("advertiser_id", advertiserId)
        .bind("campaign_id", campaignId)
        .fetch()
        .rowsUpdated()

    fun count() = this.databaseClient
        .sql("SELECT COUNT(*) FROM campaigns")
        .map { row, _ -> row.get(0) as Long }
        .one()

    fun countActive(date: Int) = this.databaseClient
        .sql("SELECT COUNT(*) FROM campaigns WHERE start_date <= :date AND end_date >= :date")
        .bind("date", date)
        .map { row, _ -> row.get(0) as Long }
        .one()

}