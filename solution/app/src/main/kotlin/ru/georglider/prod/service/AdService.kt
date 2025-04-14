package ru.georglider.prod.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import ru.georglider.prod.exceptions.model.ForbiddenException
import ru.georglider.prod.exceptions.model.NotFoundException
import ru.georglider.prod.model.advertiser.campaign.stats.RecordType
import ru.georglider.prod.model.advertiser.campaign.stats.StatRecord
import ru.georglider.prod.model.client.ClientCampaignRecord
import ru.georglider.prod.payload.dto.client.CampaignResult
import ru.georglider.prod.payload.response.client.AdResponse
import ru.georglider.prod.repository.client.AdvancedAdRepository
import ru.georglider.prod.repository.client.AdvancedRedeemRepository
import ru.georglider.prod.repository.client.AdvancedShowRepository
import ru.georglider.prod.repository.redis.AdCacheRepository
import ru.georglider.prod.service.internal.metrics.AdMetricService
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class AdService (
    private val advancedRedeemRepository: AdvancedRedeemRepository,
    private val advancedShowRepository: AdvancedShowRepository,
    private val advancedAdRepository: AdvancedAdRepository,
    private val adCacheRepository: AdCacheRepository,
    private val statService: StatService,
    private val campaignService: CampaignService,
    private val clientService: ClientService,
    private val metrics: AdMetricService
) {

    private val showRequests = ConcurrentHashMap<UUID, Int>()

    fun returnCached(clientId: UUID): Flux<Optional<String>> {
        return Flux.generate({ 0 }) { state, sink ->
            sink.next(state) // Emit request for the next index
            state + 1 // Increase the state (index)
        }.concatMap {
            adCacheRepository.takeAdById(clientId) // Fetch item only when requested
                .map { Optional.of(it) } // Wrap result
                .defaultIfEmpty(Optional.empty()) // Handle empty case
        }.takeWhile { it.isPresent } // Stop when no more items
    }

    fun show(clientId: UUID): Mono<AdResponse> {
        return clientService.findById(clientId).switchIfEmpty {
            Mono.error(NotFoundException())
        }.flatMap { client ->
            returnCached(clientId).map { it.get() }.concatMap { id ->
                advancedAdRepository.findValidById(UUID.fromString(id), client).mapNotNull { ad ->
                    val localVal = showRequests.getOrDefault(ad.campaignId, ad.impressionsLimit) + 1

                    if (localVal <= ad.impressionsLimit) {
                        showRequests[ad.campaignId] = localVal
                        return@mapNotNull ad
                    } else {
                        return@mapNotNull null
                    }
                }
            }.next().map { it as CampaignResult }.switchIfEmpty {
                val ads = advancedAdRepository.getAds(client).share()

                val firstValidAd = ads.concatMap { ad ->
                    val localVal = showRequests.getOrDefault(ad.adId, ad.impressionsAmount) + 1

                    if (localVal <= ad.maxImpressions) {
                        showRequests[ad.adId] = localVal
                        Mono.just(ad)
                    } else {
                        Mono.empty()
                    }
                }.next()
                val remainingAds = ads.skipUntilOther(firstValidAd).take(8)

                return@switchIfEmpty firstValidAd.doOnNext {
                    remainingAds.skipWhile { rAd -> rAd.adId == it.adId }.collectList().doOnNext { cache ->
                        if (cache.isNotEmpty()) {
                            adCacheRepository.saveCache(clientId, cache.map { it.adId }).subscribe()
                        }
                    }.subscribeOn(Schedulers.boundedElastic()).subscribe()
                }.map { it as CampaignResult }
            }.flatMap { ad ->
                advancedShowRepository.upsert(ClientCampaignRecord(ad.getIAdId(), clientId)).doOnNext {
                    if (it > 0L) {
                        metrics.incrementSuccessAdRequests(ad.getIRevenue())
                        statService.addStat(StatRecord(ad.getIAdvertiserId(), ad.getIAdId(), ad.getIRevenue(), RecordType.IMPRESSION))
                            .subscribeOn(Schedulers.boundedElastic()).subscribe()
                    } else {
                        metrics.incrementSuccessAdRequests()
                    }
                }.map { ad.toResponseAd() }
            }.switchIfEmpty {
                advancedAdRepository.getBackupAd(client).doOnNext {
                    metrics.incrementSuccessAdRequests()
                }.switchIfEmpty { Mono.error(NotFoundException()) }
            }
        }
    }

    fun activate(adId: UUID, clientId: UUID): Mono<UUID> {
        return Mono.just(clientId).flatMap {
            advancedShowRepository.existsByClientIdAndCampaignId(it, adId)
        }.flatMap { exists ->
            campaignService.findByCampaignId(adId).switchIfEmpty {
                Mono.error(NotFoundException())
            }.map { Pair(exists, it) }
        }.flatMap { (exists, campaign) ->
            if (!exists) return@flatMap Mono.error(ForbiddenException())
            advancedRedeemRepository.upsert(ClientCampaignRecord(
                campaign.campaignId!!, clientId
            )).doOnNext {
                if (it > 0) {
                    metrics.incrementUniqueClick(campaign.costPerClick)
                    statService.addStat(
                        StatRecord(campaign.advertiserId, campaign.campaignId!!, campaign.costPerClick, RecordType.CLICK)
                    ).subscribeOn(Schedulers.boundedElastic()).subscribe()
                }
            }.map { clientId }
        }.switchIfEmpty { Mono.error(ForbiddenException()) }
    }

}
