package ru.georglider.prod.repository.redis

import org.springframework.data.redis.core.ReactiveListOperations
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.*

@Repository
class AdCacheRepository (
    private val redisTemplate: ReactiveStringRedisTemplate
) {

    val reactiveListOps: ReactiveListOperations<String, String> = redisTemplate.opsForList()

    fun takeAdById(id: UUID): Mono<String> {
        return reactiveListOps.leftPop("cache:ads:$id")
    }

    fun saveCache(id: UUID, cache: List<UUID>): Mono<Boolean> {
        return reactiveListOps.delete("cache:ads:$id").then (
            reactiveListOps.leftPushAll("cache:ads:$id", cache.map { it.toString() }).then(
                redisTemplate.expire("cache:ads:$id", Duration.ofSeconds(60))
            )
        )
    }

}