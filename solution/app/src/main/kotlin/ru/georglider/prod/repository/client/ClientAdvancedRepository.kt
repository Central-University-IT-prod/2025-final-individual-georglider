package ru.georglider.prod.repository.client

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import ru.georglider.prod.model.client.Client

@Repository
class ClientAdvancedRepository (
    private val databaseClient: DatabaseClient
) {

    fun upsert(client: Client) = this.databaseClient
        .sql("INSERT INTO clients VALUES (:clientId, :login, :age, :location, :gender) " +
                "ON CONFLICT (id) DO UPDATE SET " +
                "login = :login, age = :age, location = :location, gender = :gender")
        .bind("clientId", client.clientId)
        .bind("login", client.login)
        .bind("age", client.age)
        .bind("location", client.location)
        .bind("gender", client.gender.ordinal)
        .fetch()
        .rowsUpdated()

}