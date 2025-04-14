package ru.georglider.prod.utils.containers

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

class AppPostgresDBContainer : PostgreSQLContainer<AppPostgresDBContainer>("postgres:latest") {

    companion object {

        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:latest"))
            .apply {
                this.withDatabaseName("prod").withUsername("root").withPassword("123456")
                this.waitingFor(Wait.forLogMessage(".*ready to accept connections.*", 1))
            }

    }

}