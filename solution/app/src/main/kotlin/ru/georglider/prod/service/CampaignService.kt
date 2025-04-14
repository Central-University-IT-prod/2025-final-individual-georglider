package ru.georglider.prod.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import ru.georglider.prod.model.advertiser.campaign.Campaign
import ru.georglider.prod.model.internal.PaginationDetails
import ru.georglider.prod.repository.CampaignRepository
import ru.georglider.prod.service.internal.metrics.CampaignMetricService
import java.util.*

@Service
class CampaignService (
    private val repository: CampaignRepository,
    private val metrics: CampaignMetricService,
    private val moderationService: ModerationService
) {

    fun save(campaign: Campaign): Mono<Campaign> {
        return if (moderationService.isEnabled()) {
            moderationService.save(campaign)
        } else {
            return if (campaign.campaignId == null) {
                repository.insert(campaign)
            } else {
                repository.update(campaign)
            }.doOnNext { metrics.refreshState() }.map {
                campaign.campaignId = it.campaignId
                campaign.costPerImpression = it.costPerImpression
                campaign.costPerClick = it.costPerClick
                campaign
            }
        }
    }

    fun setImageStatus(campaignId: UUID, imageStatus: Boolean) = repository.updateImage(campaignId, imageStatus)

    fun findByAdvertiserIdPaginated(advertiserId: UUID, paginationDetails: PaginationDetails): Flux<Campaign> {
        return repository.findByAdvertiserIdWithPagination(advertiserId, paginationDetails)
    }

    fun findByAdvertiserIdAndCampaignId(advertiserId: UUID, campaignId: UUID): Mono<Campaign> {
        return repository.findByAdvertiserIdAndCampaignId(advertiserId, campaignId)
    }

    fun existsByAdvertiserIdAndCampaignId(advertiserId: UUID, campaignId: UUID): Mono<Boolean> {
        return repository.existsByAdvertiserIdAndCampaignId(advertiserId, campaignId)
    }

    fun existsByCampaignId(campaignId: UUID): Mono<Boolean> {
        return repository.existsByCampaignId(campaignId)
    }

    fun deleteByAdvertiserIdAndCampaignId(advertiserId: UUID, campaignId: UUID): Mono<Boolean> {
        return repository.deleteByAdvertiserIdAndCampaignId(advertiserId, campaignId)
            .doOnNext {
                moderationService.deleteByAdvertiserIdAndCampaignId(advertiserId, campaignId)
                    .subscribeOn(Schedulers.boundedElastic()).subscribe()
            }
            .doOnNext { metrics.refreshState() }
            .map { it.toBoolean() }
    }

    fun findByCampaignId(campaignId: UUID): Mono<Campaign> = repository.findByCampaignId(campaignId)

    fun findAllByAdvertiserId(advertiserId: UUID): Flux<Campaign> = repository.findAllByAdvertiserId(advertiserId)

    fun Long.toBoolean(): Boolean {
        return this > 0L
    }

}
