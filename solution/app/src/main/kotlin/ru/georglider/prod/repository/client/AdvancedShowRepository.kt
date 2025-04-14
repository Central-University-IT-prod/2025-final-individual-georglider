package ru.georglider.prod.repository.client

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import ru.georglider.prod.model.client.ClientCampaignRecord
import java.util.*

@Repository
class AdvancedShowRepository (
    private val databaseClient: DatabaseClient
) {

    fun upsert(record: ClientCampaignRecord) = this.databaseClient
        .sql("INSERT INTO show_records VALUES (:client_id, :campaign_id) " +
                    "ON CONFLICT (client_id, campaign_id) DO NOTHING")
        .bind("client_id", record.clientId)
        .bind("campaign_id", record.campaignId)
        .fetch()
        .rowsUpdated()

    fun existsByClientIdAndCampaignId(clientId: UUID, campaignId: UUID) = this.databaseClient
        .sql("SELECT 1 FROM show_records WHERE client_id = :client_id AND campaign_id = :campaign_id LIMIT 1")
        .bind("client_id", clientId)
        .bind("campaign_id", campaignId)
        .map { _, _ -> true }
        .one()
        .defaultIfEmpty(false)

}