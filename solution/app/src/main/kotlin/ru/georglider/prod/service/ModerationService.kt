package ru.georglider.prod.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import ru.georglider.prod.exceptions.model.NotFoundException
import ru.georglider.prod.model.advertiser.campaign.Campaign
import ru.georglider.prod.model.internal.PaginationDetails
import ru.georglider.prod.model.moderator.ModerationRequest
import ru.georglider.prod.repository.CampaignRepository
import ru.georglider.prod.repository.internal.ModerationRepository
import ru.georglider.prod.utils.WaitPostgresUtil
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Service
class ModerationService (
    private val service: ConfigService,
    private val repository: ModerationRepository,
    private val campaignRepository: CampaignRepository
) {

    private val status: AtomicBoolean = AtomicBoolean(WaitPostgresUtil.isModerationEnabled)

    fun set(wantedStatus: Boolean): Mono<Int> {
        status.set(wantedStatus)
        val switchResult = status.get().toInt()

        return service.upsert(2, switchResult).map { switchResult }
    }

    fun isEnabled(): Boolean = status.get()

    fun findPaginated(paginationDetails: PaginationDetails): Flux<ModerationRequest> {
        return repository.findWithPagination(paginationDetails)
    }

    fun findOne() = repository.findOne()

    fun approve(id: UUID): Mono<Any> {
        return repository.findById(id).switchIfEmpty { Mono.error(NotFoundException()) }.flatMap { ad ->
            if (ad.campaign != null) {
                return@flatMap campaignRepository.insertWithId(ad.campaign).then(repository.deleteByCampaignId(ad.id))
            }
            campaignRepository.existsByCampaignId(id).flatMap {
                campaignRepository.mergeUpdate(ad)
            }.then(repository.deleteByCampaignId(ad.id))
        }
    }

    fun reject(id: UUID): Mono<Any> {
        return repository.findById(id).switchIfEmpty { Mono.error(NotFoundException()) }.flatMap { ad ->
            repository.deleteByCampaignId(ad.id)
        }
    }

    fun deleteByAdvertiserIdAndCampaignId(advertiserId: UUID, campaignId: UUID): Mono<Long> {
        return repository.deleteByAdvertiserIdAndCampaignId(advertiserId, campaignId)
    }

    fun save(campaign: Campaign): Mono<Campaign> {
        return if (campaign.campaignId == null) {
            repository.insert(campaign)
        } else {
            campaignRepository.findByCampaignId(campaign.campaignId!!).flatMap { original ->
                if (original.adText == campaign.adText && original.adTitle == campaign.adTitle) {
                    return@flatMap campaignRepository.update(campaign)
                }
                return@flatMap repository.upsert(campaign).then(Mono.just(campaign).map {
                    it.adTitle = original.adTitle
                    it.adText = original.adText
                    it
                }.flatMap { campaignRepository.update(campaign) })
            }
        }.map {
            campaign.campaignId = it.campaignId
            campaign.costPerImpression = it.costPerImpression
            campaign.costPerClick = it.costPerClick
            campaign.hasImage = it.hasImage
            campaign
        }.switchIfEmpty { Mono.error(NotFoundException()) }
    }

    fun Boolean.toInt(): Int = if (this) 1 else 0

}