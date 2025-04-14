package ru.georglider.prod.utils

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import org.slf4j.LoggerFactory


class MinioInitUtil {

    companion object {

        private val logger = LoggerFactory.getLogger(MinioInitUtil::class.java)

        @JvmStatic
        fun createBucket() {
            val minioClient =
                MinioClient.builder()
                    .endpoint("http://${System.getenv("MINIO_HOST") ?: "localhost"}:9000")
                    .credentials(
                        System.getenv("MINIO_ACCESS_KEY"),
                        System.getenv("MINIO_SECRET_KEY")
                    ).build()

            while (true) {
                try {
                    val found = minioClient.bucketExists(BucketExistsArgs.builder().bucket("bucket1").build())
                    if (found) {
                        break
                    }
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket("bucket1").build())
                } catch (ex: Exception) {
                    logger.warn("Trying to create minio bucket")
                    Thread.sleep(100) // 0.1 seconds
                }
            }

        }
    }

}