package ru.georglider.prod.utils

import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import ru.georglider.prod.config.RedisProperty

class WaitRedisUtil {

    companion object {

        private val logger = LoggerFactory.getLogger(WaitRedisUtil::class.java)

        @JvmStatic
        private fun staticRedisConnectionFactory(): RedisProperty {
            val host = System.getenv("REDIS_HOST") ?: "localhost"
            val port = System.getenv("REDIS_PORT") ?: "6379"
            return RedisProperty(host, port)
        }

        @JvmStatic
        fun connectSocket() {
            val details = staticRedisConnectionFactory()
            while (true) {
                try {
                    val conn = LettuceConnectionFactory(details.host, details.port.toInt())
                    conn.start()
                    conn.validateConnection()
                    conn.stop()
                    break
                } catch (ex: Exception) {
                    logger.warn("Trying to connect to the redis...")
                    Thread.sleep(100) // 0.1 seconds
                }
            }
        }
    }

}