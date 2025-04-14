package ru.georglider.prod.repository.client

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import ru.georglider.prod.model.client.Client
import ru.georglider.prod.payload.dto.client.AdCampaignResult
import ru.georglider.prod.payload.dto.client.RankedAd
import ru.georglider.prod.payload.response.client.AdResponse
import ru.georglider.prod.service.TimeService
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import java.util.function.BiFunction

@Repository
class AdvancedAdRepository (
    private val databaseClient: DatabaseClient,
    private val timeService: TimeService
) {

    companion object {
        val MAPPING_FUNCTION: BiFunction<Row, RowMetadata, RankedAd> =
        BiFunction<Row, RowMetadata, RankedAd> { row: Row, _: RowMetadata? ->
                RankedAd(
                    row.get("campaign_id", UUID::class.java)!!,
                    row.get("ad_title", String::class.java)!!,
                    row.get("ad_text", String::class.java)!!,
                    row.get("advertiser_id", UUID::class.java)!!,
                    row.get("has_image", Boolean::class.java)!!,
                    row.get("final_rank", BigInteger::class.java)!!.toInt(),
                    row.get("actual_impressions", Int::class.java)!!,
                    row.get("v_impression_danger_limit", Int::class.java)!!,
                    row.get("cost_per_impression", BigDecimal::class.java)!!.toDouble()
                )
            }

        val BACKUP_MAPPING_FUNCTION: BiFunction<Row, RowMetadata, AdResponse> =
            BiFunction<Row, RowMetadata, AdResponse> { row: Row, _: RowMetadata? ->
                AdResponse(
                    row.get("campaign_id", UUID::class.java)!!,
                    row.get("ad_title", String::class.java)!!,
                    row.get("ad_text", String::class.java)!!,
                    row.get("advertiser_id", UUID::class.java)!!,
                    row.get("has_image", Boolean::class.java)!!
                )
            }

        val CAMPAIGN_MAPPING_FUNCTION: BiFunction<Row, RowMetadata, AdCampaignResult> =
            BiFunction<Row, RowMetadata, AdCampaignResult> { row: Row, _: RowMetadata? ->
                AdCampaignResult(
                    row.get("campaign_id", UUID::class.java)!!,
                    row.get("advertiser_id", UUID::class.java)!!,
                    row.get("v_impression_danger_limit", Int::class.java)!!,
                    row.get("has_image", Boolean::class.java)!!,
                    row.get("actual_impressions", Int::class.java)!!,
                    row.get("cost_per_impression", BigDecimal::class.java)!!.toDouble(),
                    row.get("ad_title", String::class.java)!!,
                    row.get("ad_text", String::class.java)!!,
                )
            }
    }

    fun getAds(client: Client) = databaseClient.sql (
        """
            WITH campaign_metrics AS (
                SELECT
                    c.*,
                    COALESCE(i.impression_count, 0) as actual_impressions,
                    COALESCE(i.impression_count, 0) + 1 as projected_impressions,
                    COALESCE(cl.click_count, 0) as actual_clicks,
                    -- Revenue calculation (weight: 0.5)
                    c.cost_per_impression + c.cost_per_click / 10 as potential_revenue,
                    -- ML Score direct selection (weight: 0.25)
                    COALESCE(m.ml_score, 0) as relevance_score,
                    -- Enhanced completion rate calculations
                    CASE
                        WHEN c.impressions_limit > 0 THEN
                            ((COALESCE(i.impression_count, 0) + 1)::float / NULLIF(c.impressions_limit, 0)) * 100
                        ELSE 0
                    END as impression_percentage,
                    CASE
                        WHEN c.clicks_limit > 0 THEN
                            COALESCE(cl.click_count::float / NULLIF(c.clicks_limit, 0), 0) * 100
                        ELSE 100
                    END as click_completion,
                    CASE
                        WHEN c.impressions_limit > 0 AND (COALESCE(i.impression_count, 0) + 1) > c.impressions_limit THEN 0
                        ELSE 1
                    END as within_limits,
                    -- Calculate campaign progress percentage
                    ((:date - c.start_date)::float / GREATEST((c.end_date - c.start_date)::float, 1)) * 100 as campaign_progress,
                    -- Calculate expected progress based on time
                    CASE
                        WHEN c.impressions_limit > 0 THEN
                            ((:date - c.start_date)::float / GREATEST((c.end_date - c.start_date)::float, 1)) * c.impressions_limit
                        ELSE 0
                    END as expected_impressions
                FROM (
                    SELECT *
                    FROM campaigns
                    WHERE start_date <= :date AND end_date >= :date
                    AND v_age_from <= :age AND v_age_to >= :age
                    AND (location IS NULL OR location = :location)
                    AND (v_gender = :gender OR v_gender = 2)
                ) c
                LEFT JOIN (
                    SELECT campaign_id, COUNT(*) as impression_count
                    FROM show_records
                    GROUP BY campaign_id
                ) i ON c.campaign_id = i.campaign_id
                LEFT JOIN (
                    SELECT campaign_id, COUNT(*) as click_count
                    FROM redeem_records
                    GROUP BY campaign_id
                ) cl ON c.campaign_id = cl.campaign_id
                LEFT JOIN mlscore m ON c.advertiser_id = m.advertiser_id AND m.client_id = :client_id
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM show_records imp
                    WHERE imp.campaign_id = c.campaign_id
                    AND imp.client_id = :client_id
                )
            ),
            campaign_rankings AS (
                SELECT
                    *,
                    -- Calculate relative freshness score based on campaign progress compared to others
                    PERCENT_RANK() OVER (ORDER BY campaign_progress DESC) as progress_rank,
                    -- Calculate completion score based on how close we are to ideal progress
                    CASE
                        WHEN impression_percentage > 0 THEN
                            CASE
                                -- If we're behind schedule (less than expected impressions)
                                WHEN actual_impressions < expected_impressions THEN
                                    GREATEST(16 * (actual_impressions::float / NULLIF(expected_impressions, 0)), 0)
                                -- If we're ahead of schedule but within reasonable bounds
                                WHEN actual_impressions <= expected_impressions * 1.1 THEN
                                    16
                                -- If we're too far ahead, gradually decrease score
                                ELSE
                                    GREATEST(16 * (1 - ((actual_impressions::float / NULLIF(expected_impressions, 0)) - 1.1)), 0)
                            END
                        ELSE 16
                    END as completion_score
                FROM campaign_metrics
            )
            SELECT
                *,
                CASE
                    WHEN within_limits = 1 THEN
                        (
                            (potential_revenue / NULLIF(GREATEST(MAX(potential_revenue) OVER (), 1), 0)) * 57 +
                            (COALESCE(relevance_score::float, 0) / NULLIF(GREATEST(MAX(relevance_score) OVER (), 1), 0)) * 27 +
                            completion_score
                        ) *
                        (1 + (1 - progress_rank) * 0.5)
                    ELSE 0
                END as final_rank
            FROM campaign_rankings
            WHERE within_limits = 1
            ORDER BY final_rank DESC;
        """.trimIndent()
    ).bind("date", timeService.get())
        .bind("age", client.age)
        .bind("location", client.location)
        .bind("gender", client.gender.ordinal)
        .bind("client_id", client.clientId)
        .map(MAPPING_FUNCTION)
        .all()

    fun getBackupAd(client: Client) = databaseClient.sql(
        """
            SELECT
            c.*, m.*, r.*
            FROM (
                    SELECT *
                    FROM campaigns
                    WHERE start_date <= :date AND end_date >= :date
                    AND v_age_from <= :age AND v_age_to >= :age
                    AND (location IS NULL OR location = :location)
                    AND (v_gender = :gender OR v_gender = 2)
                ) c
            LEFT JOIN mlscore m ON c.advertiser_id = m.advertiser_id AND m.client_id = :client_id
            LEFT JOIN redeem_records r ON c.campaign_id = r.campaign_id AND r.client_id = :client_id
            WHERE EXISTS (
                SELECT 1
                FROM show_records imp
                WHERE imp.campaign_id = c.campaign_id
                AND imp.client_id = :client_id
            )
            ORDER BY r.campaign_id DESC, ml_score DESC
            LIMIT 1
        """.trimIndent()
    ).bind("date", timeService.get())
        .bind("age", client.age)
        .bind("location", client.location)
        .bind("gender", client.gender.ordinal)
        .bind("client_id", client.clientId)
        .map(BACKUP_MAPPING_FUNCTION)
        .one()


    fun findValidById(id: UUID, client: Client) = this.databaseClient.sql(
        """
            SELECT
                c.campaign_id,
                c.advertiser_id,
                c.v_impression_danger_limit,
                c.cost_per_impression,
                c.ad_title,
                c.ad_text,
                c.has_image,
                COALESCE(i.actual_impressions, 0) AS actual_impressions
            FROM (
                SELECT *
                FROM campaigns
                WHERE campaign_id = :id AND start_date <= :date AND end_date >= :date
                AND v_age_from <= :age AND v_age_to >= :age
                AND (location IS NULL OR location = :location)
                AND (v_gender = :gender OR v_gender = 2)
            ) c
            LEFT JOIN (
                SELECT
                    campaign_id,
                    COUNT(*) AS actual_impressions
                FROM
                    show_records
                GROUP BY
                    campaign_id
            ) i
            ON c.campaign_id = i.campaign_id
            WHERE
                c.campaign_id = :id;
        """.trimIndent()
    ).bind("date", timeService.get())
        .bind("age", client.age)
        .bind("location", client.location)
        .bind("gender", client.gender.ordinal)
        .bind("id", id)
        .map(CAMPAIGN_MAPPING_FUNCTION).one()

}