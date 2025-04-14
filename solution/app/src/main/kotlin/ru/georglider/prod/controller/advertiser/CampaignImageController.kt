package ru.georglider.prod.controller.advertiser

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import ru.georglider.prod.exceptions.model.BadRequestException
import ru.georglider.prod.exceptions.model.NotFoundException
import ru.georglider.prod.service.CampaignService
import ru.georglider.prod.service.internal.S3Service
import java.nio.ByteBuffer
import java.util.*


@RestController
@RequestMapping
class CampaignImageController (
    private val service: S3Service,
    private val campaignService: CampaignService,
) {

    @GetMapping("/ads/{campaignId}/image")
    fun get(@PathVariable("campaignId") rCampaignId: UUID): Mono<ResponseEntity<Flux<ByteBuffer>>> {
        return campaignService.findByCampaignId(rCampaignId).flatMap { campaign ->
            service.download(campaign.campaignId!!.toString()).map { Pair(campaign, it) }
        }.map { (campaign, response) ->
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, response.response().contentType())
                .header(HttpHeaders.CONTENT_LENGTH, response.response().contentLength().toString())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${campaign.campaignId!!}\"")
                .body(Flux.from(response))
        }.switchIfEmpty { Mono.error(NotFoundException()) }
    }

    @PostMapping("/advertisers/{advertiserId}/campaigns/{campaignId}/image")
    fun post(
        @RequestHeader headers: HttpHeaders,
        @RequestBody body: Flux<ByteBuffer>,
        @PathVariable("advertiserId") advertiserId: UUID,
        @PathVariable("campaignId") campaignId: UUID
    ): Mono<ResponseEntity<String>> {
        val length = headers.contentLength
        if (length < 0) throw BadRequestException("File length must be greater than or equal to 0")
        val mediaType: MediaType = headers.contentType ?: throw BadRequestException("File is not an image")
        if (mediaType.type != "image") throw BadRequestException("File must be image")

        return campaignService.existsByAdvertiserIdAndCampaignId(advertiserId, campaignId)
            .switchIfEmpty { Mono.error(NotFoundException()) }
            .flatMap { service.upload(body, length, mediaType.toString(), campaignId.toString()) }
            .doOnNext { campaignService.setImageStatus(campaignId, true).subscribe() }
            .map { ResponseEntity.status(201).body("Successfully uploaded an image") }
    }

    @DeleteMapping("/advertisers/{advertiserId}/campaigns/{campaignId}/image")
    fun delete(
        @PathVariable("advertiserId") advertiserId: UUID,
        @PathVariable("campaignId") campaignId: UUID
    ): Mono<ResponseEntity<String>> {
        return campaignService.existsByAdvertiserIdAndCampaignId(advertiserId, campaignId)
            .switchIfEmpty { Mono.error(NotFoundException()) }
            .flatMap { service.remove(campaignId.toString()) }
            .doOnNext { campaignService.setImageStatus(campaignId, false).subscribe() }
            .map { ResponseEntity.status(204).body("Successfully removed an image") }
    }

}