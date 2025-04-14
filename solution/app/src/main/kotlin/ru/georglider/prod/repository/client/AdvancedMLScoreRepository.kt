package ru.georglider.prod.repository.client

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import ru.georglider.prod.model.client.MLScore

@Repository
class AdvancedMLScoreRepository (
    private val databaseClient: DatabaseClient
) {

    fun upsert(mlScore: MLScore) = this.databaseClient
        .sql("INSERT INTO mlscore VALUES (:clientId, :advertiserId, :mlScore) " +
                "ON CONFLICT (client_id, advertiser_id) DO UPDATE SET " +
                "ml_score = :mlScore")
        .bind("clientId", mlScore.clientId)
        .bind("advertiserId", mlScore.advertiserId)
        .bind("mlScore", mlScore.mlScore)
        .fetch()
        .rowsUpdated()

}