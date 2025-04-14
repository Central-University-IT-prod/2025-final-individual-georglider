package ru.georglider.prod.utils.containers

import org.testcontainers.containers.MinIOContainer
import org.testcontainers.utility.DockerImageName

class AppMinioContainer : MinIOContainer("minio/minio:latest") {

    companion object {

        val minio: MinIOContainer = MinIOContainer(DockerImageName.parse("minio/minio:latest"))
            .apply {
                this.withUserName("testuser")
                this.withPassword("testpassword")
            }

    }

}