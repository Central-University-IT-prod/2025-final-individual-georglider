package ru.georglider.prod.repository.advertiser

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import ru.georglider.prod.model.advertiser.Advertiser

@Repository
class AdvertiserAdvancedRepository (
    private val databaseClient: DatabaseClient
) {

    fun upsert(advertiser: Advertiser) = this.databaseClient
        .sql("INSERT INTO advertisers VALUES (:advertiserId, :name) " +
                "ON CONFLICT (id) DO UPDATE SET " +
                "name = :name")
        .bindProperties(advertiser)
        .fetch()
        .rowsUpdated()

}