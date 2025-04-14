package ru.georglider.prod.model.internal.config

import org.springframework.boot.context.properties.ConfigurationProperties
import software.amazon.awssdk.regions.Region
import java.net.URI

@ConfigurationProperties(prefix = "aws.s3")
data class S3ClientConfigurarionProperties (
    val region: Region = Region.US_EAST_1,
    val endpoint: URI? = null,

    val accessKeyId: String? = null,
    val secretAccessKey: String? = null,

    val bucket: String? = null,

    // AWS S3 requires that file parts must have at least 5MB, except
    // for the last part. This may change for other S3-compatible services
    val multipartMinPartSize: Int = 5 * 1024 * 1024
)