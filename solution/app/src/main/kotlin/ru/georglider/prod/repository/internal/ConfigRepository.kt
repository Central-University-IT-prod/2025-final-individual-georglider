package ru.georglider.prod.repository.internal

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import ru.georglider.prod.model.internal.config.ConfigEntity
import java.util.function.BiFunction

// Repository for storing AppConfig
@Repository
class ConfigRepository (
    private val databaseClient: DatabaseClient
) {

    companion object {
        val MAPPING_FUNCTION: BiFunction<Row, RowMetadata, ConfigEntity> =
            BiFunction<Row, RowMetadata, ConfigEntity> { row: Row, _: RowMetadata? ->
                ConfigEntity(
                    row.get("id", Integer::class.java)!!.toInt(),
                    row.get("value", String::class.java)!!,
                )
            }
    }

    fun findById(id: Int) = databaseClient
        .sql("SELECT * FROM config WHERE id = :id")
        .bind("id", id)
        .map(MAPPING_FUNCTION)
        .one()

    fun insert(entity: ConfigEntity) = databaseClient
        .sql("INSERT INTO config VALUES (:id, :value)")
        .bindProperties(entity)
        .fetch()
        .rowsUpdated()

    fun update(entity: ConfigEntity) = databaseClient
        .sql("UPDATE config SET value = :value WHERE id = :id")
        .bindProperties(entity)
        .fetch()
        .rowsUpdated()

    fun upsert(entity: ConfigEntity) = databaseClient
        .sql("INSERT INTO config VALUES (:id, :value) ON CONFLICT (id) DO UPDATE SET value = :value")
        .bindProperties(entity)
        .fetch()
        .rowsUpdated()

}