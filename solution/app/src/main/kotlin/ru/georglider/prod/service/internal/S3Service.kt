package ru.georglider.prod.service.internal

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.georglider.prod.exceptions.model.NotFoundException
import ru.georglider.prod.model.internal.config.S3ClientConfigurarionProperties
import software.amazon.awssdk.core.SdkResponse
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.core.async.ResponsePublisher
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import java.nio.ByteBuffer


@Service
class S3Service (
    private val s3Client: S3AsyncClient,
    private val s3Config: S3ClientConfigurarionProperties
) {

    fun download(filekey: String): Mono<ResponsePublisher<GetObjectResponse>> {
        val request = GetObjectRequest.builder()
            .bucket(s3Config.bucket)
            .key(filekey)
            .build()

        return Mono.fromFuture(s3Client.getObject(request, AsyncResponseTransformer.toPublisher())).doOnNext { response ->
            checkResult(response.response())
        }.onErrorMap { NotFoundException() }
    }

    fun upload(body: Flux<ByteBuffer>, length: Long, contentType: String, filekey: String): Mono<PutObjectResponse> {
        return Mono.fromFuture {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(s3Config.bucket)
                    .contentLength(length)
                    .key(filekey)
                    .contentType(contentType)
                    .metadata(mapOf())
                    .build(),
                AsyncRequestBody.fromPublisher(body)
            )
        }.doOnNext { checkResult(it) }
    }

    fun remove(filekey: String): Mono<DeleteObjectResponse> {
        return Mono.fromFuture {
            s3Client.deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(s3Config.bucket)
                    .key(filekey).build()
            )
        }.doOnNext { checkResult(it) }.onErrorMap { NotFoundException() }
    }

    // Helper used to check return codes from an API call
    private fun checkResult(response: SdkResponse) {
        val sdkResponse = response.sdkHttpResponse()
        if (sdkResponse != null && sdkResponse.isSuccessful) {
            return
        }

        throw NotFoundException()
    }


}