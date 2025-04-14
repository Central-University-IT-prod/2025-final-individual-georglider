package ru.georglider.prod.utils

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Result.RowSegment
import org.slf4j.LoggerFactory
import reactor.kotlin.core.publisher.toMono
import ru.georglider.prod.repository.internal.ConfigRepository

class WaitPostgresUtil {

    companion object {

        private val logger = LoggerFactory.getLogger(WaitPostgresUtil::class.java)
        var initTime = 0
        var isModerationEnabled = false

        @JvmStatic
        private fun staticPostgresConnectionFactory(): ConnectionFactory {
            val host = System.getenv("POSTGRES_HOST") ?: "localhost"
            val user = System.getenv("POSTGRES_USER") ?: ""
            val password = System.getenv("POSTGRES_PASSWORD") ?: ""
            val port = System.getenv("POSTGRES_PORT")?.toIntOrNull() ?: 5432
            val db = System.getenv("POSTGRES_DB") ?: "prod"
            return ConnectionFactories.get("r2dbc:postgresql://$user:$password@${host}:${port}/${db}")
        }

        @JvmStatic
        fun connectSocket() {
            while (true) {
                try {
                    val conn = staticPostgresConnectionFactory().create().toMono().toFuture().join()

                    val time = conn.createStatement("SELECT * FROM config WHERE id = 1")
                        .execute().toMono()
                        .map { r -> r.filter { it is RowSegment } }
                        .flatMap { it.map(ConfigRepository.MAPPING_FUNCTION).toMono() }
                        .mapNotNull { it.getAsInt() }
                        .defaultIfEmpty(0)
                        .block()

                    val moderationStatus = conn.createStatement("SELECT * FROM config WHERE id = 2")
                        .execute().toMono()
                        .map { r -> r.filter { it is RowSegment } }
                        .flatMap { it.map(ConfigRepository.MAPPING_FUNCTION).toMono() }
                        .mapNotNull { it.getAsInt() == 1 }
                        .defaultIfEmpty(false)
                        .block()

                    conn.close().toMono().block()
                    initTime = time ?: 0
                    isModerationEnabled = moderationStatus ?: false
                    break
                } catch (ex: Exception) {
                    logger.warn("Trying to connect to the database...")
                    Thread.sleep(100) // 0.1 seconds
                }
            }
        }
    }

}