package ru.georglider.prod.utils.extensions

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MinIOContainer
import ru.georglider.prod.utils.containers.AppMinioContainer.Companion.minio

class S3SetupExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext?) {
        minio.start()
        updateDataSourceProps(minio)

        val client = MinioClient.builder()
            .endpoint(minio.s3URL)
            .credentials(
                minio.userName,
                minio.password
            ).build()

        while (true) {
            try {
                val found = client.bucketExists(BucketExistsArgs.builder().bucket("bucket1").build())
                if (found) {
                    break
                }
                client.makeBucket(MakeBucketArgs.builder().bucket("bucket1").build())
            } catch (ex: Exception) {
                Thread.sleep(100) // 0.1 seconds
            }
        }
    }

    @DynamicPropertySource
    fun updateDataSourceProps(container: MinIOContainer) {
        System.setProperty("aws.s3.endpoint", container.s3URL)
        System.setProperty("aws.s3.accessKeyId", container.userName)
        System.setProperty("aws.s3.secretAccessKey", container.password)
    }

}