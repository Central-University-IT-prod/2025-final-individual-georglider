package ru.georglider.prod.repository.client

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import ru.georglider.prod.model.client.ClientCampaignRecord

@Repository
class AdvancedRedeemRepository (
    private val databaseClient: DatabaseClient
) {

    fun upsert(record: ClientCampaignRecord) = this.databaseClient
        .sql("INSERT INTO redeem_records VALUES (:client_id, :campaign_id) " +
                    "ON CONFLICT (client_id, campaign_id) DO NOTHING")
        .bind("client_id", record.clientId)
        .bind("campaign_id", record.campaignId)
        .fetch()
        .rowsUpdated()

}