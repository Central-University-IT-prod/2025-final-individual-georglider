package ru.georglider.prod.repository.internal

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import ru.georglider.prod.model.internal.MetricHistory
import java.util.function.BiFunction

@Repository
class MetricHistoryRepository (
    private val databaseClient: DatabaseClient
) {

    companion object {
        val MAPPING_FUNCTION: BiFunction<Row, RowMetadata, MetricHistory> =
            BiFunction<Row, RowMetadata, MetricHistory> { row: Row, _: RowMetadata? ->
                MetricHistory(
                    row.get("date", Int::class.java)!!,
                    row.get("amount", Double::class.java)!!,
                    row.get("metric_name", String::class.java)!!
                )
            }
    }

    fun findAllByMetricName(metricName: String) = this.databaseClient
        .sql("SELECT * FROM metric_history WHERE metric_name = :metric_name")
        .bind("metric_name", metricName)
        .map (MAPPING_FUNCTION)
        .all()

    fun upsert(history: MetricHistory) = this.databaseClient
        .sql("INSERT INTO metric_history as mh VALUES (:date, :metricName, :amount) ON CONFLICT (date, metric_name) DO UPDATE SET amount = mh.amount + :amount")
        .bindProperties(history)
        .fetch()
        .rowsUpdated()

}