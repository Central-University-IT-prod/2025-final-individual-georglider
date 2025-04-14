package ru.georglider.prod.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories

@Configuration
@EnableRedisRepositories(basePackages = ["ru.georglider.prod.repository.redis"])
class RedisConfiguration (
    private val property: RedisProperty
) {

    @Bean
    fun getConnectionFactory(): LettuceConnectionFactory {
        return LettuceConnectionFactory(property.host, property.port.toInt())
    }

}