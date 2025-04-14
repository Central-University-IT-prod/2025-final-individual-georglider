package ru.georglider.prod.controller.advertiser

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import ru.georglider.prod.exceptions.model.BadRequestException
import ru.georglider.prod.exceptions.model.NotFoundException
import ru.georglider.prod.model.advertiser.campaign.Campaign
import ru.georglider.prod.model.internal.PaginationDetails
import ru.georglider.prod.payload.dto.advertiser.campaign.CampaignDTO
import ru.georglider.prod.payload.request.advertiser.campaign.CreateCampaignRequest
import ru.georglider.prod.payload.request.advertiser.campaign.EditCampaignRequest
import ru.georglider.prod.service.AdvertiserService
import ru.georglider.prod.service.CampaignService
import ru.georglider.prod.service.TimeService
import java.util.*

@RestController
@RequestMapping("/advertisers/{advertiserId}/campaigns")
class CampaignsController (
    private val service: CampaignService,
    private val advertiserService: AdvertiserService,
    private val timeService: TimeService
) {

    @PostMapping
    fun post(
        @PathVariable("advertiserId") advertiserId: UUID,
        @RequestBody @Valid body: CreateCampaignRequest
    ): Mono<ResponseEntity<*>> {
        if (!body.isValid()) throw BadRequestException()
        if (body.startDate!! < timeService.get()) throw BadRequestException()
        return advertiserService.existsById(advertiserId)
            .handle { it, sink -> if (it) sink.next(Campaign(advertiserId, body)) else sink.error(NotFoundException()) }
            .flatMap { service.save(it) }
            .map { CampaignDTO(it) }
            .map { ResponseEntity.status(201).body(it) }
    }
    
    @GetMapping
    fun get(
        @Min(1, message = "Страница должна начинаться с 1")
        @RequestParam(value = "page", required = false, defaultValue = "1") page: Int,
        @Min(0, message = "Размер страницы должен быть больше 0")
        @RequestParam(value = "size", required = false, defaultValue = "10") size: Int,
        @PathVariable("advertiserId") advertiserId: UUID,
    ) = advertiserService.existsById(advertiserId)
            .handle { it, sink -> if (it) sink.next(it) else sink.error(NotFoundException()) }
            .flatMap {
                service.findByAdvertiserIdPaginated(advertiserId, PaginationDetails(page, size))
                    .map { CampaignDTO(it) }
                    .collectList()
            }

    @GetMapping("/{campaignId}")
    fun getCampaign(
        @PathVariable("advertiserId") advertiserId: UUID,
        @PathVariable("campaignId") campaignId: UUID
    ) = service.findByAdvertiserIdAndCampaignId(advertiserId, campaignId)
            .map { CampaignDTO(it) }.switchIfEmpty { Mono.error(NotFoundException()) }

    @PutMapping("/{campaignId}")
    fun editCampaign(
        @PathVariable("advertiserId") advertiserId: UUID,
        @PathVariable("campaignId") campaignId: UUID,
        @RequestBody @Valid body: EditCampaignRequest
    ): Mono<CampaignDTO> {
        if (!body.isValid()) throw BadRequestException()
        return service.findByAdvertiserIdAndCampaignId(advertiserId, campaignId)
            .switchIfEmpty { Mono.error(NotFoundException()) }
            .handle { it, sink -> if (!body.canBeMerged(timeService.get(), it)) sink.error(BadRequestException()) else sink.next(it) }
            .flatMap { service.save(Campaign(advertiserId, campaignId, body)) }
            .map { CampaignDTO(it) }
    }

    @DeleteMapping("/{campaignId}")
    fun deleteCampaign(
        @PathVariable("advertiserId") advertiserId: UUID,
        @PathVariable("campaignId") campaignId: UUID
    ): Mono<ResponseEntity<*>> {
        return advertiserService.existsById(advertiserId)
            .handle { it, sink -> if (it) sink.next(it) else sink.error(NotFoundException()) }
            .flatMap { service.deleteByAdvertiserIdAndCampaignId(advertiserId, campaignId) }
            .map { if (it) 204 else 404 }
            .map { ResponseEntity.status(it).body(null) }
    }


}