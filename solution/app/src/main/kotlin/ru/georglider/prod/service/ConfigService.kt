package ru.georglider.prod.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.georglider.prod.model.internal.config.ConfigEntity
import ru.georglider.prod.repository.internal.ConfigRepository

@Service
class ConfigService (
    private val configRepository: ConfigRepository,
) {

    fun get(id: Int): Mono<ConfigEntity> = configRepository.findById(id)
    fun upsert(id: Int, value: String) = configRepository.upsert(ConfigEntity(id, value))
    fun upsert(id: Int, value: Int) = configRepository.upsert(ConfigEntity(id, value.toString()))

}