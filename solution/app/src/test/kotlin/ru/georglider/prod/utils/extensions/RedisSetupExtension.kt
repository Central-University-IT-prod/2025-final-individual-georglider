package ru.georglider.prod.utils.extensions

import com.redis.testcontainers.RedisContainer
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.test.context.DynamicPropertySource
import ru.georglider.prod.utils.containers.AppRedisContainer.Companion.redis

class RedisSetupExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext?) {
        redis.start()
        updateDataSourceProps(redis)
    }

    @DynamicPropertySource
    fun updateDataSourceProps(container: RedisContainer) {
        System.setProperty("spring.data.redis.host", container.host)
        System.setProperty("spring.data.redis.port", container.firstMappedPort.toString())
    }

}