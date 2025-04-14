package ru.georglider.prod.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "spring.data.redis", ignoreInvalidFields = true)
data class RedisProperty(
    var host: String = "",
    var port: String = ""
)