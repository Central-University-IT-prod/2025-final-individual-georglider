package ru.georglider.prod.utils.extensions

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import ru.georglider.prod.utils.containers.AppPostgresDBContainer.Companion.postgres

class DatabaseSetupExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext?) {
        postgres.start()
        updateDataSourceProps(postgres)
    }

    @DynamicPropertySource
    fun updateDataSourceProps(container: PostgreSQLContainer<*>) {
        System.setProperty("spring.r2dbc.url", r2dbc(container))
        System.setProperty("spring.r2dbc.username", container.username)
        System.setProperty("spring.r2dbc.password", container.password)
    }

    companion object {
        @JvmStatic
        private fun r2dbc(container: PostgreSQLContainer<*>): String {
            return "r2dbc:postgresql://${container.host}:${container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)}/${container.databaseName}"
        }
    }

}