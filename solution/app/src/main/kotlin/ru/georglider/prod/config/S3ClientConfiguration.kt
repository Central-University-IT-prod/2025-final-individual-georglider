package ru.georglider.prod.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.georglider.prod.model.internal.config.S3ClientConfigurarionProperties
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.utils.StringUtils
import java.time.Duration


@Configuration
@EnableConfigurationProperties(S3ClientConfigurarionProperties::class)
class S3ClientConfiguration {

    @Bean
    fun s3client(
        s3props: S3ClientConfigurarionProperties,
        credentialsProvider: AwsCredentialsProvider
    ): S3AsyncClient {
        val httpClient: SdkAsyncHttpClient = NettyNioAsyncHttpClient.builder()
            .writeTimeout(Duration.ZERO)
            .maxConcurrency(64)
            .build()
        val serviceConfiguration: S3Configuration = S3Configuration.builder()
            .chunkedEncodingEnabled(true)
            .pathStyleAccessEnabled(true)
            .build()
        val builder: S3AsyncClientBuilder = S3AsyncClient.builder().httpClient(httpClient)
            .endpointOverride(s3props.endpoint)
            .region(s3props.region)
            .credentialsProvider(credentialsProvider)
            .serviceConfiguration(serviceConfiguration)
        return builder.build()
    }

    @Bean
    fun awsCredentialsProvider(s3props: S3ClientConfigurarionProperties): AwsCredentialsProvider {
        return if (StringUtils.isBlank(s3props.accessKeyId)) {
            DefaultCredentialsProvider.create()
        } else {
            AwsCredentialsProvider {
                AwsBasicCredentials.create(
                    s3props.accessKeyId,
                    s3props.secretAccessKey
                )
            }
        }
    }

}