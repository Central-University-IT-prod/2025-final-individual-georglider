package ru.georglider.prod.utils.containers

import com.redis.testcontainers.RedisContainer
import org.testcontainers.utility.DockerImageName

class AppRedisContainer : RedisContainer("redis:latest") {

    companion object {
        val redis: RedisContainer = RedisContainer(DockerImageName.parse("redis:latest"))
    }

}